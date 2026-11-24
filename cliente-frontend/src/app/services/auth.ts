import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpContext, HttpContextToken, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, shareReplay, switchMap, tap, throwError } from 'rxjs';

import { LOGIN_URL, PERFIL_URL } from '../config/api';

/** Roles definidos por Spring Security en el backend. */
export type Rol = 'ROLE_USER' | 'ROLE_GESTOR' | 'ROLE_ADMIN';

export const ROLES: readonly Rol[] = ['ROLE_USER', 'ROLE_GESTOR', 'ROLE_ADMIN'];

export const ETIQUETAS_ROL: Record<Rol, string> = {
  ROLE_USER: 'Solicitante',
  ROLE_GESTOR: 'Gestor de recursos',
  ROLE_ADMIN: 'Administrador'
};

/**
 * Cuerpo devuelto por POST /login (JwtAuthenticationFilter.successfulAuthentication).
 * Todos los campos se tratan como opcionales: el token oficial viaja en la cabecera Authorization.
 */
export interface LoginRespuesta {
  token?: string;
  type?: string;
  username?: string;
  idUsuario?: number | null;
  nombre?: string;
  rol?: string;
  authorities?: string[];
  roles?: string[];
  documento?: string;
  email?: string;
  expiresIn?: number;
  message?: string;
}

/** Usuario devuelto por GET /api/usuarios/me (entidad Usuario; la contraseña nunca se serializa). */
export interface UsuarioAutenticado {
  idUsuario: number;
  nombre: string;
  tipo?: string;
  codigo?: string;
  documento?: string | null;
  email?: string | null;
  username: string;
  rol: string;
}

/**
 * Permite que una petición maneje su propio 401 sin que el interceptor redirija al login
 * (lo usa la verificación de sesión, cuya redirección la decide el guard).
 */
export const OMITIR_REDIRECCION_401 = new HttpContextToken<boolean>(() => false);

const CLAVE_TOKEN = 'campusrecursos.token';
const PREFIJO_BEARER = 'Bearer ';
/** Tiempo durante el cual se reutiliza el perfil validado antes de volver a consultar /me. */
const VIGENCIA_VERIFICACION_MS = 30_000;

export function normalizarRol(rol: string | null | undefined): Rol | null {
  if (!rol) return null;
  const valor = rol.trim().toUpperCase();
  const conPrefijo = valor.startsWith('ROLE_') ? valor : `ROLE_${valor}`;
  return (ROLES as readonly string[]).includes(conPrefijo) ? (conPrefijo as Rol) : null;
}

export function etiquetaRol(rol: string | null | undefined): string {
  const r = normalizarRol(rol);
  return r ? ETIQUETAS_ROL[r] : 'Sin rol';
}

/**
 * Servicio de autenticación de la SPA.
 * - El backend es la fuente oficial de autenticación y autorización.
 * - Solo se persiste el JWT (sessionStorage); nunca la contraseña ni el rol.
 * - El rol vigente se obtiene siempre de GET /api/usuarios/me.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _token = signal<string | null>(this.leerTokenGuardado());
  private readonly _usuario = signal<UsuarioAutenticado | null>(null);
  private ultimaVerificacion = 0;
  private verificacionEnCurso: Observable<boolean> | null = null;

  readonly token = this._token.asReadonly();
  readonly usuario = this._usuario.asReadonly();

  readonly estaAutenticado = computed(() => !!this._token() && !!this._usuario());
  readonly rol = computed<Rol | null>(() => normalizarRol(this._usuario()?.rol));
  readonly esAdmin = computed(() => this.rol() === 'ROLE_ADMIN');
  readonly esGestor = computed(() => this.rol() === 'ROLE_GESTOR');
  readonly esGestorOAdmin = computed(() => this.esAdmin() || this.esGestor());
  readonly esSolicitante = computed(() => this.rol() === 'ROLE_USER');

  /** Inicia sesión contra POST /login y luego recupera el perfil vigente con /me. */
  login(username: string, password: string): Observable<UsuarioAutenticado> {
    this.limpiarSesion();
    return this.http
      .post<LoginRespuesta>(LOGIN_URL, { username: username.trim(), password }, { observe: 'response' })
      .pipe(
        map((respuesta) => this.extraerToken(respuesta)),
        tap((token) => this.guardarToken(token)),
        switchMap(() => this.cargarPerfil()),
        catchError((error) => {
          this.limpiarSesion();
          return throwError(() => error);
        })
      );
  }

  /**
   * Confirma con el backend que la sesión sigue siendo válida y actualiza el rol vigente.
   * Reutiliza el resultado durante unos segundos para no consultar /me en cada navegación.
   */
  verificarSesion(forzar = false): Observable<boolean> {
    const token = this._token();
    if (!token) return of(false);

    if (this.tokenExpirado(token)) {
      this.limpiarSesion();
      return of(false);
    }

    const reciente = Date.now() - this.ultimaVerificacion < VIGENCIA_VERIFICACION_MS;
    if (!forzar && this._usuario() && reciente) return of(true);

    if (!this.verificacionEnCurso) {
      this.verificacionEnCurso = this.cargarPerfil().pipe(
        map(() => true),
        catchError((error: unknown) => {
          if (error instanceof HttpErrorResponse && (error.status === 401 || error.status === 404)) {
            this.limpiarSesion();
            return of(false);
          }
          // Backend no disponible u otro error: se conserva el token y, si ya había un perfil
          // validado, se mantiene; el backend seguirá rechazando cualquier petición no autorizada.
          return of(!!this._usuario());
        }),
        finalize(() => (this.verificacionEnCurso = null)),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.verificacionEnCurso;
  }

  /** Consulta GET /api/usuarios/me y actualiza el usuario en memoria. */
  cargarPerfil(): Observable<UsuarioAutenticado> {
    const context = new HttpContext().set(OMITIR_REDIRECCION_401, true);
    return this.http.get<UsuarioAutenticado>(PERFIL_URL, { context }).pipe(
      tap((usuario) => {
        this._usuario.set(usuario);
        this.ultimaVerificacion = Date.now();
      })
    );
  }

  tieneAlgunRol(roles: readonly string[]): boolean {
    const actual = this.rol();
    if (!actual) return false;
    return roles.some((r) => normalizarRol(r) === actual);
  }

  /** Ruta de inicio después de autenticarse. */
  rutaInicio(): string {
    return '/catalogo';
  }

  /** Cierra la sesión local (el backend es stateless: basta con descartar el JWT). */
  cerrarSesion(): void {
    this.limpiarSesion();
    void this.router.navigate(['/login']);
  }

  /** Elimina el token y el perfil en memoria sin navegar. */
  limpiarSesion(): void {
    this._token.set(null);
    this._usuario.set(null);
    this.ultimaVerificacion = 0;
    try {
      sessionStorage.removeItem(CLAVE_TOKEN);
    } catch {
      /* sessionStorage no disponible */
    }
  }

  private extraerToken(respuesta: HttpResponse<LoginRespuesta>): string {
    const cabecera = respuesta.headers.get('Authorization');
    if (cabecera?.startsWith(PREFIJO_BEARER)) {
      return cabecera.slice(PREFIJO_BEARER.length).trim();
    }
    const token = respuesta.body?.token;
    if (token) return token;
    throw new Error('El servidor no devolvió un token de acceso.');
  }

  private guardarToken(token: string): void {
    this._token.set(token);
    try {
      sessionStorage.setItem(CLAVE_TOKEN, token);
    } catch {
      /* sessionStorage no disponible: la sesión dura lo que dure la pestaña en memoria */
    }
  }

  private leerTokenGuardado(): string | null {
    try {
      return sessionStorage.getItem(CLAVE_TOKEN);
    } catch {
      return null;
    }
  }

  /** Lee el claim exp del JWT solo para evitar enviar tokens vencidos; la validación real es del backend. */
  private tokenExpirado(token: string): boolean {
    try {
      const payload = token.split('.')[1];
      if (!payload) return true;
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const json = JSON.parse(atob(base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')));
      return typeof json.exp === 'number' && json.exp * 1000 <= Date.now();
    } catch {
      return true;
    }
  }
}
