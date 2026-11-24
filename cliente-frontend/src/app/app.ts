import { Component, computed, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService, etiquetaRol } from './services/auth';
import { NotificacionService } from './services/notificacion';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('cliente-frontend');
  protected readonly auth = inject(AuthService);
  protected readonly notificaciones = inject(NotificacionService);

  protected readonly nombreUsuario = computed(() => this.auth.usuario()?.nombre ?? '');
  protected readonly etiquetaRol = computed(() => etiquetaRol(this.auth.usuario()?.rol));
  protected readonly inicial = computed(() => (this.nombreUsuario().trim().charAt(0) || '?').toUpperCase());

  cerrarSesion(): void {
    this.notificaciones.cerrar();
    this.auth.cerrarSesion();
  }
}
