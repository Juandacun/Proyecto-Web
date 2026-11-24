import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminUsuariosService, UsuarioAdmin, UsuarioEdicion, UsuarioNuevo } from '../admin-usuarios.service';
import { AuthService, ETIQUETAS_ROL, ROLES, Rol, etiquetaRol, normalizarRol } from '../../services/auth';

type Modal = 'ninguno' | 'detalle' | 'formulario' | 'rol' | 'eliminar';
type ModoFormulario = 'crear' | 'editar';
type FiltroRol = Rol | 'TODOS';

interface FormularioUsuario {
  nombre: string;
  tipo: string;
  codigo: string;
  documento: string;
  email: string;
  username: string;
  password: string;
  confirmarPassword: string;
  rol: Rol;
}

export const TIPOS_USUARIO = ['Estudiante', 'Docente', 'Investigador', 'Administrativo'] as const;

export const DESCRIPCION_ROL: Record<Rol, string> = {
  ROLE_USER: 'Consulta el catálogo, gestiona sus propias reservas y préstamos y reporta incidentes.',
  ROLE_GESTOR: 'Gestiona reservas, entregas, devoluciones, incidentes, mantenimiento y el tablero operativo.',
  ROLE_ADMIN: 'Todo lo del gestor, más la administración de usuarios, roles y recursos.'
};

const PATRON_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PATRON_IDENTIFICADOR = /^[A-Za-z0-9._@-]+$/;
const MIN_PASSWORD = 6;
const MAX_PASSWORD = 72; // límite de BCrypt

@Component({
  selector: 'app-admin-usuarios',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './admin-usuarios.html',
  styleUrl: './admin-usuarios.css'
})
export class AdminUsuariosComponent implements OnInit {
  private readonly servicio = inject(AdminUsuariosService);
  private readonly auth = inject(AuthService);

  readonly roles = ROLES;
  readonly tipos = TIPOS_USUARIO;
  readonly etiquetas = ETIQUETAS_ROL;
  readonly descripciones = DESCRIPCION_ROL;

  readonly usuarios = signal<UsuarioAdmin[]>([]);
  readonly cargando = signal(false);
  readonly errorCarga = signal<string | null>(null);
  readonly mensaje = signal<{ tipo: 'exito' | 'error'; texto: string } | null>(null);

  readonly busqueda = signal('');
  readonly filtroRol = signal<FiltroRol>('TODOS');

  readonly modal = signal<Modal>('ninguno');
  readonly modo = signal<ModoFormulario>('crear');
  readonly seleccionado = signal<UsuarioAdmin | null>(null);
  readonly cargandoDetalle = signal(false);
  readonly guardando = signal(false);
  readonly errorModal = signal<string | null>(null);

  form: FormularioUsuario = this.formularioVacio();
  rolNuevo: Rol = 'ROLE_USER';

  readonly idPropio = computed(() => this.auth.usuario()?.idUsuario ?? null);

  readonly conteos = computed(() => {
    const lista = this.usuarios();
    const porRol = (rol: Rol) => lista.filter((u) => normalizarRol(u.rol) === rol).length;
    return { TODOS: lista.length, ROLE_USER: porRol('ROLE_USER'), ROLE_GESTOR: porRol('ROLE_GESTOR'), ROLE_ADMIN: porRol('ROLE_ADMIN') };
  });

  readonly visibles = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    const rol = this.filtroRol();
    return this.usuarios()
      .filter((u) => rol === 'TODOS' || normalizarRol(u.rol) === rol)
      .filter((u) => {
        if (!texto) return true;
        return [u.nombre, u.username, u.codigo, u.email, u.documento, u.tipo]
          .some((campo) => (campo ?? '').toString().toLowerCase().includes(texto));
      })
      .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    this.servicio.listar().subscribe({
      next: (lista) => {
        this.usuarios.set(lista);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.cargando.set(false);
        this.errorCarga.set(this.mensajeError(err, 'No se pudo cargar la lista de usuarios.'));
      }
    });
  }

  // ---------- utilidades de vista ----------

  esPropio(u: UsuarioAdmin | null): boolean {
    return !!u && u.idUsuario === this.idPropio();
  }

  etiquetaRol(rol: string | null | undefined): string {
    return etiquetaRol(rol);
  }

  claseRol(rol: string | null | undefined): string {
    switch (normalizarRol(rol)) {
      case 'ROLE_ADMIN': return 'badge badge-admin';
      case 'ROLE_GESTOR': return 'badge badge-gestor';
      case 'ROLE_USER': return 'badge badge-user';
      default: return 'badge';
    }
  }

  cambiarFiltroRol(rol: FiltroRol): void {
    this.filtroRol.set(rol);
  }

  cerrarMensaje(): void {
    this.mensaje.set(null);
  }

  cerrarModal(): void {
    if (this.guardando()) return;
    this.modal.set('ninguno');
    this.seleccionado.set(null);
    this.errorModal.set(null);
    this.form = this.formularioVacio();
  }

  // ---------- consultar ----------

  verDetalle(u: UsuarioAdmin): void {
    this.errorModal.set(null);
    this.seleccionado.set(u);
    this.modal.set('detalle');
    this.cargandoDetalle.set(true);
    this.servicio.obtener(u.idUsuario).subscribe({
      next: (detalle) => {
        this.seleccionado.set(detalle);
        this.cargandoDetalle.set(false);
      },
      error: (err: unknown) => {
        this.cargandoDetalle.set(false);
        this.errorModal.set(this.mensajeError(err, 'No se pudo consultar el usuario.'));
      }
    });
  }

  // ---------- crear / editar ----------

  abrirCrear(): void {
    this.modo.set('crear');
    this.seleccionado.set(null);
    this.form = this.formularioVacio();
    this.errorModal.set(null);
    this.modal.set('formulario');
  }

  abrirEditar(u: UsuarioAdmin): void {
    this.modo.set('editar');
    this.seleccionado.set(u);
    this.form = {
      nombre: u.nombre ?? '',
      tipo: u.tipo ?? 'Estudiante',
      codigo: u.codigo ?? '',
      documento: u.documento ?? '',
      email: u.email ?? '',
      username: u.username ?? '',
      password: '',
      confirmarPassword: '',
      rol: normalizarRol(u.rol) ?? 'ROLE_USER'
    };
    this.errorModal.set(null);
    this.modal.set('formulario');
  }

  guardar(): void {
    if (this.guardando()) return;
    const error = this.validarFormulario();
    if (error) {
      this.errorModal.set(error);
      return;
    }
    this.errorModal.set(null);
    this.guardando.set(true);

    const f = this.form;
    const password = f.password.trim() ? f.password : undefined;

    if (this.modo() === 'crear') {
      const nuevo: UsuarioNuevo = {
        nombre: f.nombre.trim(),
        tipo: f.tipo,
        codigo: f.codigo.trim(),
        documento: f.documento.trim() || undefined,
        email: f.email.trim() || undefined,
        username: f.username.trim() || undefined,
        password: f.password,
        rol: f.rol
      };
      this.servicio.crear(nuevo).subscribe({
        next: (creado) => this.alGuardar(`Usuario "${creado.nombre}" creado con rol ${etiquetaRol(creado.rol)}.`),
        error: (err: unknown) => this.alFallarGuardado(err)
      });
      return;
    }

    const actual = this.seleccionado();
    if (!actual) {
      this.guardando.set(false);
      return;
    }
    const edicion: UsuarioEdicion = {
      nombre: f.nombre.trim(),
      tipo: f.tipo,
      codigo: f.codigo.trim(),
      documento: f.documento.trim(),
      email: f.email.trim(),
      // El propio administrador no cambia su usuario de acceso: invalidaría su JWT.
      username: this.esPropio(actual) ? actual.username : f.username.trim() || f.codigo.trim(),
      // Se reenvía el rol actual para no degradarlo; el cambio de rol usa PATCH /{id}/rol.
      rol: actual.rol,
      ...(password ? { password } : {})
    };
    this.servicio.actualizar(actual.idUsuario, edicion).subscribe({
      next: (actualizado) => {
        if (this.esPropio(actualizado)) this.auth.verificarSesion(true).subscribe();
        this.alGuardar(`Datos de "${actualizado.nombre}" actualizados.`);
      },
      error: (err: unknown) => this.alFallarGuardado(err)
    });
  }

  // ---------- roles ----------

  abrirCambioRol(u: UsuarioAdmin): void {
    if (this.esPropio(u)) return;
    this.seleccionado.set(u);
    this.rolNuevo = normalizarRol(u.rol) ?? 'ROLE_USER';
    this.errorModal.set(null);
    this.modal.set('rol');
  }

  confirmarCambioRol(): void {
    const u = this.seleccionado();
    if (!u || this.guardando()) return;
    if (this.esPropio(u)) {
      this.errorModal.set('No puedes modificar tu propio rol.');
      return;
    }
    if (!ROLES.includes(this.rolNuevo)) {
      this.errorModal.set('Selecciona un rol válido.');
      return;
    }
    if (normalizarRol(u.rol) === this.rolNuevo) {
      this.errorModal.set('El usuario ya tiene ese rol.');
      return;
    }
    this.guardando.set(true);
    this.servicio.cambiarRol(u.idUsuario, this.rolNuevo).subscribe({
      next: (actualizado) =>
        this.alGuardar(`Rol de "${actualizado.nombre}" cambiado a ${etiquetaRol(actualizado.rol)}.`),
      error: (err: unknown) => this.alFallarGuardado(err)
    });
  }

  // ---------- eliminar ----------

  abrirEliminar(u: UsuarioAdmin): void {
    if (this.esPropio(u)) return;
    this.seleccionado.set(u);
    this.errorModal.set(null);
    this.modal.set('eliminar');
  }

  confirmarEliminar(): void {
    const u = this.seleccionado();
    if (!u || this.guardando() || this.esPropio(u)) return;
    this.guardando.set(true);
    this.servicio.eliminar(u.idUsuario).subscribe({
      next: () => this.alGuardar(`Usuario "${u.nombre}" eliminado.`),
      error: (err: unknown) => this.alFallarGuardado(err)
    });
  }

  // ---------- privados ----------

  private alGuardar(texto: string): void {
    this.guardando.set(false);
    this.cerrarModal();
    this.mensaje.set({ tipo: 'exito', texto });
    this.cargar();
  }

  private alFallarGuardado(err: unknown): void {
    this.guardando.set(false);
    this.errorModal.set(this.mensajeError(err, 'No se pudo completar la operación.'));
  }

  private validarFormulario(): string | null {
    const f = this.form;
    const creando = this.modo() === 'crear';
    const idActual = this.seleccionado()?.idUsuario ?? null;

    if (!f.nombre.trim()) return 'El nombre es obligatorio.';
    if (f.nombre.trim().length > 120) return 'El nombre no puede superar 120 caracteres.';
    if (!f.codigo.trim()) return 'El código institucional es obligatorio.';
    if (!PATRON_IDENTIFICADOR.test(f.codigo.trim())) return 'El código solo admite letras, números, punto, guion y guion bajo.';
    if (!(TIPOS_USUARIO as readonly string[]).includes(f.tipo)) return 'Selecciona un tipo de usuario válido.';
    if (f.email.trim() && !PATRON_EMAIL.test(f.email.trim())) return 'El correo electrónico no es válido.';
    if (f.username.trim() && !PATRON_IDENTIFICADOR.test(f.username.trim())) {
      return 'El usuario de acceso no puede tener espacios ni caracteres especiales.';
    }
    if (f.documento.trim() && !/^[A-Za-z0-9-]+$/.test(f.documento.trim())) return 'El documento solo admite letras, números y guiones.';

    if (creando && !f.password) return 'La contraseña es obligatoria para crear el usuario.';
    if (f.password) {
      if (f.password.length < MIN_PASSWORD) return `La contraseña debe tener al menos ${MIN_PASSWORD} caracteres.`;
      if (f.password.length > MAX_PASSWORD) return `La contraseña no puede superar ${MAX_PASSWORD} caracteres.`;
      // El backend no vuelve a cifrar valores que empiezan por "$2" (los trata como hash BCrypt).
      if (f.password.startsWith('$2')) return 'La contraseña no puede empezar por "$2".';
      if (f.password !== f.confirmarPassword) return 'Las contraseñas no coinciden.';
    }
    if (creando && !ROLES.includes(f.rol)) return 'Selecciona un rol válido.';

    const otros = this.usuarios().filter((u) => u.idUsuario !== idActual);
    const igual = (a: string | null | undefined, b: string) => !!a && a.trim().toLowerCase() === b.trim().toLowerCase();
    if (otros.some((u) => igual(u.codigo, f.codigo))) return 'Ya existe un usuario con ese código.';
    const username = f.username.trim() || f.codigo.trim();
    if (otros.some((u) => igual(u.username, username) || igual(u.codigo, username) || igual(u.email, username))) {
      return 'Ese usuario de acceso ya está en uso.';
    }
    if (f.email.trim() && otros.some((u) => igual(u.email, f.email) || igual(u.username, f.email))) {
      return 'Ya existe un usuario con ese correo.';
    }
    return null;
  }

  private mensajeError(err: unknown, porDefecto: string): string {
    if (err instanceof HttpErrorResponse) {
      const cuerpo = err.error;
      const texto =
        cuerpo && typeof cuerpo === 'object'
          ? ((cuerpo as Record<string, unknown>)['error'] ?? (cuerpo as Record<string, unknown>)['message'])
          : null;
      if (err.status === 0) return 'No se pudo conectar con el servidor.';
      if (err.status === 403) return 'Acceso denegado: esta operación requiere rol de administrador.';
      if (err.status === 404) return typeof texto === 'string' ? texto : 'El usuario ya no existe.';
      if (err.status === 400 && typeof texto === 'string') return texto;
      if (err.status === 409 || err.status >= 500) {
        return 'No se pudo guardar. Verifica que el código, el usuario de acceso y el correo no estén registrados.';
      }
      if (typeof texto === 'string' && texto.trim()) return texto;
    }
    return porDefecto;
  }

  private formularioVacio(): FormularioUsuario {
    return {
      nombre: '',
      tipo: 'Estudiante',
      codigo: '',
      documento: '',
      email: '',
      username: '',
      password: '',
      confirmarPassword: '',
      rol: 'ROLE_USER'
    };
  }
}
