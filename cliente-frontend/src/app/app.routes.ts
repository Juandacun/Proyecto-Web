import { Routes } from '@angular/router';
import { CatalogoComponent } from './components/catalogo/catalogo';
import { PrestamosComponent } from './components/prestamos/prestamos';
import { MantenimientoComponent } from './components/mantenimiento/mantenimiento';
import { ReservasComponent } from './components/reservas/reservas';
import { TableroComponent } from './components/tablero/tablero';
import { LoginComponent } from './components/login/login';
import { AccesoDenegadoComponent } from './components/acceso-denegado/acceso-denegado';
import { authGuard, invitadoGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'catalogo', pathMatch: 'full' },
  { path: 'login', component: LoginComponent, canActivate: [invitadoGuard], title: 'Iniciar sesión · CampusRecursos' },
  { path: 'acceso-denegado', component: AccesoDenegadoComponent, canActivate: [authGuard], title: 'Acceso denegado · CampusRecursos' },

  { path: 'catalogo', component: CatalogoComponent, canActivate: [authGuard] },
  { path: 'reservas', component: ReservasComponent, canActivate: [authGuard] },
  { path: 'prestamos', component: PrestamosComponent, canActivate: [authGuard] },
  { path: 'mantenimiento', component: MantenimientoComponent, canActivate: [authGuard] },
  {
    path: 'tablero',
    component: TableroComponent,
    canActivate: [roleGuard],
    data: { roles: ['ROLE_GESTOR', 'ROLE_ADMIN'] }
  },
  {
    path: 'admin',
    canActivate: [roleGuard],
    data: { roles: ['ROLE_ADMIN'] },
    loadChildren: () => import('./admin/admin.routes').then((m) => m.ADMIN_ROUTES)
  },

  { path: '**', redirectTo: 'catalogo' }
];
