import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from '../services/auth';

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
