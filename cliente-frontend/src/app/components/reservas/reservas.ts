import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, debounceTime, of, switchMap, catchError } from 'rxjs';

import { Disponibilidad, Reserva, ReservaDTO, ReservaService } from '../../services/reserva';
import { RecursoService } from '../../services/recurso';
import { Usuario, UsuarioService } from '../../services/usuario';

type Tab = 'proximas' | 'historial' | 'todas';
type ModoFormulario = 'crear' | 'editar';

interface FormularioReserva {
  recursoId: number | '';
  fecha: string;
  horaInicio: string;
  horaFin: string;
  motivo: string;
  solicitante: string;
}

@Component({
  selector: 'app-reservas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reservas.html',
  styleUrls: ['./reservas.css']
})
export class ReservasComponent implements OnInit {
  private readonly reservaService = inject(ReservaService);
  private readonly recursoService = inject(RecursoService);
  private readonly usuarioService = inject(UsuarioService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly reservas = signal<Reserva[]>([]);
  readonly espacios = signal<any[]>([]);
  readonly usuarios = signal<Usuario[]>([]);
  readonly cargando = signal(false);

  readonly tab = signal<Tab>('proximas');
  readonly filtroRecursoId = signal<number | ''>('');
  readonly filtroFecha = signal('');
  readonly filtroTexto = signal('');

  readonly reservasVisibles = computed(() => {
    const texto = this.filtroTexto().trim().toLowerCase();
    const recursoId = this.filtroRecursoId();
    const fecha = this.filtroFecha();
    const tab = this.tab();

    const lista = this.reservas().filter((r) => {
      if (tab === 'proximas' && !(r.estado === 'Confirmada' && !this.haTerminado(r))) return false;
      if (tab === 'historial' && r.estado === 'Confirmada' && !this.haTerminado(r)) return false;
      if (recursoId !== '' && r.recurso?.idRecurso !== Number(recursoId)) return false;
      if (fecha && r.fecha !== fecha) return false;
      if (texto) {
        const enTexto = [r.recurso?.nombre, r.solicitante, r.motivo]
          .filter(Boolean)
          .some((v) => String(v).toLowerCase().includes(texto));
        if (!enTexto) return false;
      }
      return true;
    });

    const clave = (r: Reserva) => `${r.fecha} ${r.horaInicio}`;
    return [...lista].sort((a, b) =>
      tab === 'proximas' ? clave(a).localeCompare(clave(b)) : clave(b).localeCompare(clave(a))
    );
  });

  readonly totalProximas = computed(
    () => this.reservas().filter((r) => r.estado === 'Confirmada' && !this.haTerminado(r)).length
  );

  readonly mensajeExito = signal<string | null>(null);
  readonly mensajeError = signal<string | null>(null);

  readonly mostrarFormulario = signal(false);
  readonly modo = signal<ModoFormulario>('crear');
  readonly reservaEditando = signal<Reserva | null>(null);
  readonly guardando = signal(false);
  readonly errorFormulario = signal<string | null>(null);
  readonly disponibilidad = signal<Disponibilidad | null>(null);
  readonly verificando = signal(false);
  readonly ocupacionDia = signal<Reserva[]>([]);

  form: FormularioReserva = this.formularioVacio();

  readonly horas: string[] = Array.from({ length: 33 }, (_, i) => {
    const minutos = 6 * 60 + i * 30;
    return `${String(Math.floor(minutos / 60)).padStart(2, '0')}:${String(minutos % 60).padStart(2, '0')}`;
  });

  readonly hoy = this.fechaLocal(new Date());

  readonly reservaACancelar = signal<Reserva | null>(null);
  readonly cancelando = signal(false);

  private readonly cambiosFormulario$ = new Subject<void>();

  ngOnInit(): void {
    this.cargarReservas();
    this.cargarEspacios();
    this.cargarUsuarios();
    this.configurarVerificacionEnVivo();

    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const recursoId = Number(params.get('recursoId'));
      if (recursoId) {
        this.abrirCrear(recursoId);
      }
    });
  }

  cargarReservas(): void {
    this.cargando.set(true);
    this.reservaService.listar().subscribe({
      next: (data) => {
        this.reservas.set(data);
        this.cargando.set(false);
      },
      error: (err) => {
        console.error(err);
        this.mensajeError.set('No se pudieron cargar las reservas. ¿Está corriendo el backend en el puerto 8080?');
        this.cargando.set(false);
      }
    });
  }

  private cargarEspacios(): void {
    this.recursoService.obtenerRecursos({ clase: 'ESPACIO' }).subscribe({
      next: (data) => this.espacios.set(data),
      error: (err) => console.error('Error al cargar espacios', err)
    });
  }

  private cargarUsuarios(): void {
    this.usuarioService.listar().subscribe({
      next: (data) => this.usuarios.set(data),
      error: () => this.usuarios.set([])
    });
  }

  cambiarTab(tab: Tab): void {
    this.tab.set(tab);
  }

  limpiarFiltros(): void {
    this.filtroRecursoId.set('');
    this.filtroFecha.set('');
    this.filtroTexto.set('');
  }

  abrirCrear(recursoId?: number): void {
    this.limpiarMensajes();
    this.modo.set('crear');
    this.reservaEditando.set(null);
    this.form = this.formularioVacio();
    if (recursoId) this.form.recursoId = recursoId;
    this.errorFormulario.set(null);
    this.disponibilidad.set(null);
    this.ocupacionDia.set([]);
    this.mostrarFormulario.set(true);
    this.onCambioFormulario();
  }

  abrirEditar(reserva: Reserva): void {
    if (!this.esEditable(reserva)) return;
    this.limpiarMensajes();
    this.modo.set('editar');
    this.reservaEditando.set(reserva);
    this.form = {
      recursoId: reserva.recurso.idRecurso,
      fecha: reserva.fecha,
      horaInicio: reserva.horaInicio,
      horaFin: reserva.horaFin,
      motivo: reserva.motivo ?? '',
      solicitante: reserva.solicitante ?? ''
    };
    this.errorFormulario.set(null);
    this.disponibilidad.set(null);
    this.mostrarFormulario.set(true);
    this.onCambioFormulario();
  }

  cerrarFormulario(): void {
    this.mostrarFormulario.set(false);
    this.reservaEditando.set(null);
    this.errorFormulario.set(null);
    this.disponibilidad.set(null);
    this.ocupacionDia.set([]);
  }

  onCambioFormulario(): void {
    this.errorFormulario.set(null);
    if (this.form.horaInicio && (!this.form.horaFin || this.form.horaFin <= this.form.horaInicio)) {
      const idx = this.horas.indexOf(this.form.horaInicio);
      if (idx >= 0 && idx + 2 < this.horas.length) {
        this.form.horaFin = this.horas[idx + 2];
      }
    }
    this.cambiosFormulario$.next();
  }

  get formularioCompleto(): boolean {
    const f = this.form;
    return !!(f.recursoId && f.fecha && f.horaInicio && f.horaFin);
  }

  get espacioSeleccionado(): any | null {
    return this.espacios().find((e) => e.idRecurso === Number(this.form.recursoId)) ?? null;
  }

  private validarEnCliente(): string | null {
    const f = this.form;
    if (!this.formularioCompleto) return 'Selecciona el espacio, la fecha y el horario.';
    if (f.horaInicio >= f.horaFin) return 'La hora de inicio debe ser anterior a la hora de fin.';
    const inicio = new Date(`${f.fecha}T${f.horaInicio}:00`);
    if (inicio.getTime() <= Date.now()) return 'No se puede reservar en una fecha u hora que ya pasó.';
    return null;
  }

  private configurarVerificacionEnVivo(): void {
    this.cambiosFormulario$
      .pipe(
        debounceTime(300),
        switchMap(() => {
          this.cargarOcupacionDia();
          const errorLocal = this.validarEnCliente();
          if (errorLocal) {
            this.verificando.set(false);
            this.disponibilidad.set(
              this.formularioCompleto ? { disponible: false, error: errorLocal } : null
            );
            return of(null);
          }
          this.verificando.set(true);
          return this.reservaService
            .verificarDisponibilidad(this.dtoDesdeFormulario(), this.reservaEditando()?.idReserva)
            .pipe(
              catchError((err: HttpErrorResponse) =>
                of<Disponibilidad>({ disponible: false, error: this.extraerError(err) })
              )
            );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((resultado) => {
        this.verificando.set(false);
        if (resultado) this.disponibilidad.set(resultado);
      });
  }

  private cargarOcupacionDia(): void {
    const recursoId = Number(this.form.recursoId);
    const fecha = this.form.fecha;
    if (!recursoId || !fecha) {
      this.ocupacionDia.set([]);
      return;
    }
    const idEditando = this.reservaEditando()?.idReserva;
    this.reservaService.listarPorRecursoYFecha(recursoId, fecha).subscribe({
      next: (data) =>
        this.ocupacionDia.set(
          data
            .filter((r) => r.estado === 'Confirmada' && r.idReserva !== idEditando)
            .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio))
        ),
      error: () => this.ocupacionDia.set([])
    });
  }

  guardar(): void {
    const errorLocal = this.validarEnCliente();
    if (errorLocal) {
      this.errorFormulario.set(errorLocal);
      return;
    }

    this.guardando.set(true);
    this.errorFormulario.set(null);
    const dto = this.dtoDesdeFormulario();
    const editando = this.reservaEditando();
    const peticion = editando
      ? this.reservaService.actualizar(editando.idReserva, dto)
      : this.reservaService.crear(dto);

    peticion.subscribe({
      next: (reserva) => {
        this.guardando.set(false);
        this.cerrarFormulario();
        this.mensajeExito.set(
          editando
            ? `Reserva #${reserva.idReserva} modificada: ${reserva.recurso.nombre}, ${this.formatearFecha(reserva.fecha)} de ${reserva.horaInicio} a ${reserva.horaFin}.`
            : `Reserva #${reserva.idReserva} confirmada: ${reserva.recurso.nombre}, ${this.formatearFecha(reserva.fecha)} de ${reserva.horaInicio} a ${reserva.horaFin}.`
        );
        this.cargarReservas();
      },
      error: (err: HttpErrorResponse) => {
        this.guardando.set(false);
        this.errorFormulario.set(this.extraerError(err));
        this.onCambioFormulario();
      }
    });
  }

  pedirCancelacion(reserva: Reserva): void {
    if (!this.esEditable(reserva)) return;
    this.limpiarMensajes();
    this.reservaACancelar.set(reserva);
  }

  cerrarCancelacion(): void {
    this.reservaACancelar.set(null);
  }

  confirmarCancelacion(): void {
    const reserva = this.reservaACancelar();
    if (!reserva) return;
    this.cancelando.set(true);
    this.reservaService.cancelar(reserva.idReserva).subscribe({
      next: () => {
        this.cancelando.set(false);
        this.reservaACancelar.set(null);
        this.mensajeExito.set(`Reserva #${reserva.idReserva} cancelada. El horario quedó libre.`);
        this.cargarReservas();
      },
      error: (err: HttpErrorResponse) => {
        this.cancelando.set(false);
        this.reservaACancelar.set(null);
        this.mensajeError.set(this.extraerError(err));
        this.cargarReservas();
      }
    });
  }

  finalizar(reserva: Reserva): void {
    this.limpiarMensajes();
    this.reservaService.finalizar(reserva.idReserva).subscribe({
      next: () => {
        this.mensajeExito.set(`Reserva #${reserva.idReserva} marcada como finalizada.`);
        this.cargarReservas();
      },
      error: (err: HttpErrorResponse) => this.mensajeError.set(this.extraerError(err))
    });
  }

  esEditable(r: Reserva): boolean {
    if (r.editable !== undefined) return r.editable;
    return r.estado === 'Confirmada' && new Date(`${r.fecha}T${r.horaInicio}:00`).getTime() > Date.now();
  }

  esFinalizable(r: Reserva): boolean {
    return r.estado === 'Confirmada' && !this.esEditable(r);
  }

  haTerminado(r: Reserva): boolean {
    return new Date(`${r.fecha}T${r.horaFin}:00`).getTime() <= Date.now();
  }

  claseEstado(r: Reserva): string {
    if (r.estado === 'Cancelada') return 'tag--rojo';
    if (r.estado === 'Finalizada') return 'tag--gris';
    if (r.estado === 'Confirmada' && this.haTerminado(r)) return 'tag--ambar';
    return 'tag--verde';
  }

  textoEstado(r: Reserva): string {
    if (r.estado === 'Confirmada' && this.haTerminado(r)) return 'Confirmada · por finalizar';
    if (r.estado === 'Confirmada' && !this.esEditable(r)) return 'En curso';
    return r.estado;
  }

  formatearFecha(fecha: string): string {
    if (!fecha) return '';
    const [a, m, d] = fecha.split('-').map(Number);
    return new Date(a, m - 1, d).toLocaleDateString('es-CO', {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    });
  }

  private dtoDesdeFormulario(): ReservaDTO {
    return {
      recursoId: Number(this.form.recursoId),
      fecha: this.form.fecha,
      horaInicio: this.form.horaInicio,
      horaFin: this.form.horaFin,
      motivo: this.form.motivo?.trim() || undefined,
      solicitante: this.form.solicitante?.trim() || undefined
    };
  }

  private extraerError(err: HttpErrorResponse): string {
    if (err.status === 0) return 'No hay conexión con el servidor (http://localhost:8080).';
    const cuerpo = err.error;
    if (cuerpo && typeof cuerpo === 'object' && (cuerpo.error || cuerpo.mensaje)) {
      return cuerpo.error || cuerpo.mensaje;
    }
    if (typeof cuerpo === 'string' && cuerpo.length < 300) return cuerpo;
    if (err.status === 404) return 'La reserva ya no existe.';
    return 'Ocurrió un error inesperado al procesar la reserva.';
  }

  private formularioVacio(): FormularioReserva {
    return { recursoId: '', fecha: '', horaInicio: '', horaFin: '', motivo: '', solicitante: '' };
  }

  private limpiarMensajes(): void {
    this.mensajeExito.set(null);
    this.mensajeError.set(null);
  }

  private fechaLocal(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
}
