import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthService, etiquetaRol } from '../../services/auth';

@Component({
  selector: 'app-acceso-denegado',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="denegado">
      <div class="card">
        <div class="codigo">403</div>
        <h1>Acceso denegado</h1>
        <p>
          Tu rol actual (<strong>{{ rol() }}</strong>) no tiene permiso para ver
          @if (desde) { <code>{{ desde }}</code> } @else { esta sección }.
        </p>
        <p class="nota">Tu sesión sigue activa. Si crees que necesitas acceso, contacta al administrador del sistema.</p>
        <a class="btn" [routerLink]="inicio">Volver al catálogo</a>
      </div>
    </section>
  `,
  styles: `
    .denegado { display: flex; justify-content: center; padding: 48px 16px; }
    .card { max-width: 480px; width: 100%; padding: 32px; text-align: center; background: #fff; border: 1px solid #E2E8F0; border-radius: 12px; }
    .codigo { font-size: 40px; font-weight: 700; color: #DC2626; }
    h1 { margin: 8px 0 12px; font-size: 20px; color: #0F172A; }
    p { margin: 0 0 12px; font-size: 14px; line-height: 21px; color: #334155; }
    .nota { color: #64748B; font-size: 13px; }
    code { padding: 2px 6px; border-radius: 4px; background: #F1F5F9; font-size: 13px; }
    .btn { display: inline-block; margin-top: 8px; padding: 10px 16px; border-radius: 6px; background: #2563EB; color: #fff; font-size: 14px; font-weight: 600; text-decoration: none; }
    .btn:hover { background: #1D4ED8; color: #fff; }
  `
})
export class AccesoDenegadoComponent {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  readonly rol = computed(() => etiquetaRol(this.auth.usuario()?.rol));
  readonly desde = this.route.snapshot.queryParamMap.get('desde');
  readonly inicio = this.auth.rutaInicio();
}
