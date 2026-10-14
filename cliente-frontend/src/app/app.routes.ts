import { Routes } from '@angular/router';
import { CatalogoComponent } from './components/catalogo/catalogo';
import { PrestamosComponent } from './components/prestamos/prestamos';
import { MantenimientoComponent } from './components/mantenimiento/mantenimiento';

export const routes: Routes = [
  { path: '', redirectTo: 'catalogo', pathMatch: 'full' },
  { path: 'catalogo', component: CatalogoComponent },
  { path: 'prestamos', component: PrestamosComponent },
  { path: 'mantenimiento', component: MantenimientoComponent },
  { path: '**', redirectTo: 'catalogo' }
];
