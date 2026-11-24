import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../../services/auth';
import { esRutaInternaSegura } from '../../config/api';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  username = '';
  password = '';
  readonly mostrarPassword = signal(false);
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);
  readonly aviso = signal<string | null>(this.avisoInicial());

  iniciarSesion(): void {
    if (this.enviando()) return;
    const username = this.username.trim();
    if (!username || !this.password) {
      this.error.set('Ingresa tu usuario y tu contraseña.');
      return;
    }

    this.error.set(null);
    this.aviso.set(null);
    this.enviando.set(true);

    this.auth.login(username, this.password).subscribe({
      next: () => {
        this.password = '';
        this.enviando.set(false);
        const destino = this.route.snapshot.queryParamMap.get('returnUrl');
        void this.router.navigateByUrl(esRutaInternaSegura(destino) ? destino : this.auth.rutaInicio());
      },
      error: (err: unknown) => {
        this.password = '';
        this.enviando.set(false);
        this.error.set(this.mensajeError(err));
      }
    });
  }

  alternarPassword(): void {
    this.mostrarPassword.update((v) => !v);
  }

  private mensajeError(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 401) return 'Usuario o contraseña incorrectos.';
      if (err.status === 0) {
        return 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución (localhost:8080).';
      }
      return 'No fue posible iniciar sesión en este momento. Intenta de nuevo.';
    }
    return 'No fue posible iniciar sesión. Intenta de nuevo.';
  }

  private avisoInicial(): string | null {
    const motivo = this.route.snapshot.queryParamMap.get('motivo');
    if (motivo === 'expirada') return 'Tu sesión expiró o ya no es válida. Inicia sesión de nuevo.';
    if (motivo === 'requerida') return 'Debes iniciar sesión para continuar.';
    return null;
  }
}
