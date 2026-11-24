import { Routes } from '@angular/router';

/**
 * Módulo de administración (carga diferida). La ruta padre /admin ya está protegida
 * con roleGuard + ROLE_ADMIN en app.routes.ts; el backend exige ROLE_ADMIN en /api/usuarios/**.
 */
export const ADMIN_ROUTES: Routes = [
  { path: '', redirectTo: 'usuarios', pathMatch: 'full' },
  {
    path: 'usuarios',
    loadComponent: () => import('./usuarios/admin-usuarios').then((m) => m.AdminUsuariosComponent),
    title: 'Usuarios y roles · CampusRecursos'
  }
];
