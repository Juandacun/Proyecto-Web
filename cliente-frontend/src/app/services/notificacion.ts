import { Injectable, signal } from '@angular/core';

export type TipoNotificacion = 'error' | 'exito' | 'info';

export interface Notificacion {
  id: number;
  texto: string;
  tipo: TipoNotificacion;
}

/**
 * Mensajes globales de la aplicación (por ejemplo, 403 en una operación).
 * Se muestran en el layout principal y se ocultan solos.
 */
@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private readonly _actual = signal<Notificacion | null>(null);
  readonly actual = this._actual.asReadonly();

  private secuencia = 0;
  private temporizador: ReturnType<typeof setTimeout> | null = null;

  mostrar(texto: string, tipo: TipoNotificacion = 'info', duracionMs = 6000): void {
    const id = ++this.secuencia;
    this._actual.set({ id, texto, tipo });
    if (this.temporizador) clearTimeout(this.temporizador);
    this.temporizador = setTimeout(() => {
      if (this._actual()?.id === id) this._actual.set(null);
    }, duracionMs);
  }

  cerrar(): void {
    if (this.temporizador) clearTimeout(this.temporizador);
    this.temporizador = null;
    this._actual.set(null);
  }
}
