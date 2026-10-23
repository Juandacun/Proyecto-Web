import { Routes } from '@angular/router';
import { CatalogoComponent } from './components/catalogo/catalogo';
import { PrestamosComponent } from './components/prestamos/prestamos';
import { MantenimientoComponent } from './components/mantenimiento/mantenimiento';
import { ReservasComponent } from './components/reservas/reservas';
import { TableroComponent } from './components/tablero/tablero';

export const routes: Routes = [
  { path: '', redirectTo: 'catalogo', pathMatch: 'full' },
  { path: 'catalogo', component: CatalogoComponent },
  { path: 'reservas', component: ReservasComponent },
  { path: 'prestamos', component: PrestamosComponent },
  { path: 'mantenimiento', component: MantenimientoComponent },
  { path: 'tablero', component: TableroComponent },
  { path: '**', redirectTo: 'catalogo' }
];
