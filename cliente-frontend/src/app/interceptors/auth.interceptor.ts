import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { esRutaInternaSegura, esUrlDeLogin, esUrlDelBackend } from '../config/api';
import { AuthService, OMITIR_REDIRECCION_401 } from '../services/auth';
import { NotificacionService } from '../services/notificacion';

const MENSAJE_403 = 'Acceso denegado: tu rol no tiene permiso para realizar esta operación.';

/**
 * Interceptor JWT.
 * - Adjunta "Authorization: Bearer <token>" solo a peticiones dirigidas al backend de CampusRecursos.
 * - Nunca adjunta el token al endpoint de login ni a dominios externos.
 * - 401 en una petición protegida: limpia la sesión y redirige al login.
 * - 401 en el login: no hace nada (el formulario muestra "credenciales incorrectas").
 * - 403: conserva la sesión y el JWT; muestra un mensaje de acceso denegado.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const notificaciones = inject(NotificacionService);

  const esBackend = esUrlDelBackend(req.url);
  const esLogin = esUrlDeLogin(req.url);
  const token = auth.token();

  const peticion =
    esBackend && !esLogin && token && !req.headers.has('Authorization')
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(peticion).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && esBackend && !esLogin) {
        if (error.status === 401) {
          const teniaSesion = !!auth.token();
          auth.limpiarSesion();
          if (!req.context.get(OMITIR_REDIRECCION_401)) {
            const actual = router.url;
            void router.navigate(['/login'], {
              queryParams: {
                motivo: teniaSesion ? 'expirada' : 'requerida',
                returnUrl: esRutaInternaSegura(actual) ? actual : null
              }
            });
          }
        } else if (error.status === 403 && req.method !== 'GET') {
          // Las consultas (GET) rechazadas las maneja cada vista; las operaciones muestran aviso global.
          notificaciones.mostrar(mensajeDelServidor(error) ?? MENSAJE_403, 'error');
        }
      }
      return throwError(() => error);
    })
  );
};

function mensajeDelServidor(error: HttpErrorResponse): string | null {
  const cuerpo = error.error;
  if (cuerpo && typeof cuerpo === 'object') {
    const texto = (cuerpo as Record<string, unknown>)['message'] ?? (cuerpo as Record<string, unknown>)['error'];
    if (typeof texto === 'string' && texto.trim()) return `Acceso denegado: ${texto}`;
  }
  return null;
}
