import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthService, Rol } from '../services/auth';

/**
 * RoleGuard (funcional): exige que el rol vigente (según /api/usuarios/me) esté en route.data['roles'].
 *
 * Uso:
 *   { path: 'tablero', canActivate: [roleGuard], data: { roles: ['ROLE_GESTOR', 'ROLE_ADMIN'] } }
 *
 * Es solo control de navegación: Spring Security sigue siendo quien autoriza cada endpoint.
 */
export const roleGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const rolesPermitidos = (route.data['roles'] ?? []) as readonly Rol[];

  // Al validar la ruta se fuerza la consulta del rol vigente para que un cambio de rol se refleje de inmediato.
  return auth.verificarSesion(true).pipe(
    map((valida) => {
      if (!valida) {
        return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
      }
      if (rolesPermitidos.length === 0 || auth.tieneAlgunRol(rolesPermitidos)) {
        return true;
      }
      return router.createUrlTree(['/acceso-denegado'], { queryParams: { desde: state.url } });
    })
  );
};
