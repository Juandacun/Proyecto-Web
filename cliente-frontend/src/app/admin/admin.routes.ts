import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  { path: '', redirectTo: 'usuarios', pathMatch: 'full' },
  {
    path: 'usuarios',
    loadComponent: () => import('./usuarios/admin-usuarios').then((m) => m.AdminUsuariosComponent),
    title: 'Usuarios y roles · CampusRecursos'
  }
];
