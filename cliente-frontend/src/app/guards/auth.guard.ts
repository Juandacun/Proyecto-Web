import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from '../services/auth';

/**
 * AuthGuard (funcional): exige una sesión válida confirmada por el backend (GET /api/usuarios/me).
 * Si no hay sesión, redirige al login conservando la ruta solicitada.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth
    .verificarSesion()
    .pipe(map((valida) => (valida ? true : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } }))));
};

/** Impide abrir el login cuando ya hay una sesión válida. */
export const invitadoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.verificarSesion().pipe(map((valida) => (valida ? router.parseUrl(auth.rutaInicio()) : true)));
};
