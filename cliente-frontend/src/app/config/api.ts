export const API_BASE_URL = 'http://localhost:8080';

/** Endpoint de autenticación expuesto por JwtAuthenticationFilter. */
export const LOGIN_URL = `${API_BASE_URL}/login`;

/** Perfil del usuario autenticado (rol vigente según la base de datos). */
export const PERFIL_URL = `${API_BASE_URL}/api/usuarios/me`;

/** Indica si la URL apunta al backend de CampusRecursos (mismo origen que API_BASE_URL). */
export function esUrlDelBackend(url: string): boolean {
  try {
    const base = typeof window !== 'undefined' ? window.location.origin : API_BASE_URL;
    return new URL(url, base).origin === new URL(API_BASE_URL).origin;
  } catch {
    return false;
  }
}

/** Indica si la URL corresponde al endpoint de login del backend. */
export function esUrlDeLogin(url: string): boolean {
  try {
    const destino = new URL(url, API_BASE_URL);
    return esUrlDelBackend(url) && destino.pathname.replace(/\/+$/, '') === '/login';
  } catch {
    return false;
  }
}

/** Valida que una ruta de retorno sea interna a la SPA (evita redirecciones abiertas). */
export function esRutaInternaSegura(ruta: string | null | undefined): ruta is string {
  return (
    !!ruta &&
    ruta.startsWith('/') &&
    !ruta.startsWith('//') &&
    !ruta.startsWith('/\\') &&
    !ruta.startsWith('/login') &&
    !ruta.startsWith('/acceso-denegado')
  );
}
