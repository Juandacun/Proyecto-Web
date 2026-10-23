import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, switchMap, catchError, of, Subject, Subscription } from 'rxjs';
import { PrestamoService, PrestamoEntregaDTO, PrestamoDevolucionDTO } from '../../services/prestamo';
import { RecursoService } from '../../services/recurso';
import { UsuarioService, Usuario } from '../../services/usuario';

@Component({
  selector: 'app-prestamos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './prestamos.html',
  styleUrls: ['./prestamos.css']
})
export class PrestamosComponent implements OnInit, OnDestroy {
  prestamos: any[] = [];
  todosLosPrestamos: any[] = []; // Caché en memoria para filtrado y respuesta instantánea (0ms)
  equiposDisponibles: any[] = [];
  usuarios: Usuario[] = [];

  tabSeleccionada: 'todos' | 'activos' | 'tardios' = 'activos';

  cargando = false;
  mensajeExito: string | null = null;
  mensajeError: string | null = null;

  // Pipeline reactivo para cambio de pestañas con cancelación automática de peticiones previas (switchMap)
  private tabChange$ = new Subject<'todos' | 'activos' | 'tardios'>();
  private tabSubscription?: Subscription;

  // Horas del campus (07:00 a 21:00)
  horasCampus: string[] = [
    '07:00', '08:00', '09:00', '10:00', '11:00', '12:00',
    '13:00', '14:00', '15:00', '16:00', '17:00', '18:00',
    '19:00', '20:00', '21:00'
  ];

  fechaMinima: string = new Date().toISOString().split('T')[0];

  // Modal / Form Entrega
  mostrarModalEntrega = false;
  equipoSeleccionado: any = null;
  usuarioSeleccionadoId: number | '' = '';

  fechaEntregaDate: string = '';
  horaEntregaTime: string = '';

  fechaLimiteDate: string = '';
  horaLimiteTime: string = '18:00';

  entregaForm: PrestamoEntregaDTO = {
    recursoId: 0,
    usuarioResponsable: '',
    idUsuario: '',
    usuarioId: undefined,
    fechaEntrega: '',
    fechaLimite: '',
    estadoInicial: 'Operativo',
    observaciones: '',
    accesorios: 'Cables, cargador, estuche'
  };
  guardandoEntrega = false;
  errorModalEntrega: string | null = null;

  // Modal Devolución
  prestamoParaDevolucion: any = null;
  fechaDevolucionDate: string = '';
  horaDevolucionTime: string = '12:00';
  errorModalDevolucion: string | null = null;
  devolucionForm: PrestamoDevolucionDTO = {
    fechaDevolucion: '',
    estadoFinal: 'Buen estado',
    observaciones: ''
  };
  guardandoDevolucion = false;

  constructor(
    private prestamoService: PrestamoService,
    private recursoService: RecursoService,
    private usuarioService: UsuarioService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.iniciarFlujoPestanas();
    this.cargarPrestamos();
    this.cargarEquipos();
    this.cargarUsuarios();
  }

  ngOnDestroy(): void {
    if (this.tabSubscription) {
      this.tabSubscription.unsubscribe();
    }
    this.tabChange$.complete();
  }

  /**
   * Configura el flujo reactivo con switchMap:
   * 1. Cancela automáticamente peticiones previas en curso si el usuario cambia rápido de pestaña.
   * 2. Aplica primero el filtro en memoria (0ms) para que la pantalla no se congele.
   * 3. Apaga this.cargando = false de inmediato y fuerza la detección de cambios (cdr.detectChanges).
   */
  private iniciarFlujoPestanas(): void {
    this.tabSubscription = this.tabChange$
      .pipe(
        switchMap((tab) => {
          this.tabSeleccionada = tab;
          this.mensajeError = null;

          // Si ya tenemos datos en memoria, actualizamos la vista de inmediato
          this.aplicarFiltroEnMemoria(tab);

          // Si aún no hay datos en memoria, mostramos el indicador de carga
          if (!this.todosLosPrestamos || this.todosLosPrestamos.length === 0) {
            this.cargando = true;
          }
          this.cdr.detectChanges();

          let request$;
          if (tab === 'activos') {
            request$ = this.prestamoService.listarActivos();
          } else if (tab === 'tardios') {
            request$ = this.prestamoService.listarTardios();
          } else {
            request$ = this.prestamoService.listar();
          }

          return request$.pipe(
            catchError((err) => {
              console.error(`Error al consultar préstamos (${tab}):`, err);
              this.mensajeError =
                err?.error?.message || err?.error?.error || err?.message || 'Error al conectar con el servidor.';
              return of(null);
            }),
            finalize(() => {
              this.cargando = false;
              this.cdr.detectChanges();
            })
          );
        })
      )
      .subscribe((data) => {
        this.cargando = false;
        if (data !== null && Array.isArray(data)) {
          this.prestamos = data;
          if (this.tabSeleccionada === 'todos') {
            this.todosLosPrestamos = [...data];
          }
        } else if (this.todosLosPrestamos.length > 0) {
          this.aplicarFiltroEnMemoria(this.tabSeleccionada);
        }
        this.cdr.detectChanges();
      });
  }

  /**
   * Filtra instantáneamente la lista en memoria para responder en 0ms sin bloqueos
   */
  private aplicarFiltroEnMemoria(tab: 'todos' | 'activos' | 'tardios'): void {
    if (!this.todosLosPrestamos || this.todosLosPrestamos.length === 0) {
      return;
    }
    if (tab === 'activos') {
      this.prestamos = this.todosLosPrestamos.filter((p) => {
        const est = (p.estado || '').toLowerCase();
        return !p.fechaDevolucion && !est.startsWith('devuelt');
      });
    } else if (tab === 'tardios') {
      const hoy = new Date().toISOString().split('T')[0];
      this.prestamos = this.todosLosPrestamos.filter((p) => {
        if (p.puntualidad === 'Tardía' || p.puntualidad === 'Vencido') {
          return true;
        }
        const limite = (p.fechaDevolucionEstimada || p.fechaLimite || '').split('T')[0];
        const noDevuelto = !p.fechaDevolucion && !(p.estado || '').toLowerCase().startsWith('devuelt');
        return noDevuelto && limite && limite < hoy;
      });
    } else {
      this.prestamos = [...this.todosLosPrestamos];
    }
  }

  cargarPrestamos(): void {
    // Si no tenemos la lista completa en memoria, la precargamos en background
    this.prestamoService.listar().subscribe({
      next: (todos) => {
        if (Array.isArray(todos)) {
          this.todosLosPrestamos = todos;
          if (this.tabSeleccionada !== 'todos') {
            this.aplicarFiltroEnMemoria(this.tabSeleccionada);
            this.cdr.detectChanges();
          }
        }
      },
      error: () => {}
    });

    // Emite la carga de la pestaña activa a través de switchMap
    this.tabChange$.next(this.tabSeleccionada);
  }

  cargarEquipos(): void {
    this.recursoService.listarEquipos().subscribe({
      next: (data) => {
        this.equiposDisponibles = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error al cargar equipos', err)
    });
  }

  cargarUsuarios(): void {
    this.usuarioService.listar().subscribe({
      next: (data) => {
        this.usuarios = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.warn('Backend /api/usuarios no disponible aún, usando datos institucionales de reserva.', err);
        this.usuarios = [
          { idUsuario: 1, nombre: 'Juan Pérez', tipo: 'Estudiante', codigo: 'EST-202410' },
          { idUsuario: 2, nombre: 'Dra. María Gómez', tipo: 'Docente', codigo: 'DOC-108234' },
          { idUsuario: 3, nombre: 'Carlos Rodríguez', tipo: 'Investigador', codigo: 'INV-309112' },
          { idUsuario: 4, nombre: 'Ing. Ana Torres', tipo: 'Administrativo', codigo: 'ADM-401928' },
          { idUsuario: 5, nombre: 'Laura Martínez', tipo: 'Estudiante', codigo: 'EST-202411' },
          { idUsuario: 6, nombre: 'David Morales', tipo: 'Estudiante', codigo: 'EST-202412' }
        ];
        this.cdr.detectChanges();
      }
    });
  }

  cambiarTab(tab: 'todos' | 'activos' | 'tardios'): void {
    this.tabSeleccionada = tab;
    // Filtrado en memoria instantáneo
    this.aplicarFiltroEnMemoria(tab);
    this.cdr.detectChanges();
    // Dispara la petición a través de switchMap (cancela peticiones HTTP anteriores en vuelo)
    this.tabChange$.next(tab);
  }

  // --- Modal Entrega (RN-02, RN-03) ---
  abrirModalEntrega(): void {
    this.cargarEquipos();
    this.cargarUsuarios();
    this.mostrarModalEntrega = true;
    this.guardandoEntrega = false;
    this.equipoSeleccionado = null;
    this.usuarioSeleccionadoId = '';
    this.errorModalEntrega = null;

    // Fecha y hora de entrega precargadas automáticamente con el momento actual
    const ahora = new Date();
    this.fechaEntregaDate = ahora.toISOString().split('T')[0];
    const horas = String(ahora.getHours()).padStart(2, '0');
    const minutos = String(ahora.getMinutes()).padStart(2, '0');
    this.horaEntregaTime = `${horas}:${minutos}`;

    // Fecha límite de devolución por defecto: mañana a las 18:00
    const manana = new Date(Date.now() + 86400000);
    this.fechaLimiteDate = manana.toISOString().split('T')[0];
    this.horaLimiteTime = '18:00';

    this.entregaForm = {
      recursoId: 0,
      usuarioResponsable: '',
      idUsuario: '',
      usuarioId: undefined,
      fechaEntrega: '',
      fechaLimite: '',
      estadoInicial: 'Operativo',
      observaciones: '',
      accesorios: 'Cables, cargador, estuche'
    };
    this.cdr.detectChanges();
  }

  cerrarModalEntrega(): void {
    this.mostrarModalEntrega = false;
    this.guardandoEntrega = false;
    this.equipoSeleccionado = null;
    this.usuarioSeleccionadoId = '';
    this.errorModalEntrega = null;
    this.cdr.detectChanges();
  }

  onSeleccionarEquipo(): void {
    this.errorModalEntrega = null;
    const id = Number(this.entregaForm.recursoId);
    this.equipoSeleccionado = this.equiposDisponibles.find((e) => e.idRecurso === id) || null;
    this.cdr.detectChanges();
  }

  onSeleccionarUsuario(): void {
    this.errorModalEntrega = null;
    const id = Number(this.usuarioSeleccionadoId);
    const usuario = this.usuarios.find((u) => u.idUsuario === id);
    if (usuario) {
      // Vincula nombre, tipo y código para el registro
      this.entregaForm.usuarioResponsable = `${usuario.nombre} - ${usuario.tipo} (${usuario.codigo})`;
    } else {
      this.entregaForm.usuarioResponsable = '';
    }
    this.cdr.detectChanges();
  }

  private normalizarHora(hora: string): string {
    if (!hora) return '00:00';
    const partes = hora.split(':');
    const h = partes[0].padStart(2, '0');
    const m = (partes[1] || '00').padStart(2, '0');
    return `${h}:${m}`;
  }

  // Validación cronológica en tiempo real: entrega vs devolución
  get esHorarioInvalido(): boolean {
    if (!this.fechaEntregaDate || !this.horaEntregaTime || !this.fechaLimiteDate || !this.horaLimiteTime) {
      return false;
    }
    // Si la fecha límite es anterior al día de entrega
    if (this.fechaLimiteDate < this.fechaEntregaDate) {
      return true;
    }
    // Si es el mismo día, la hora límite debe ser estrictamente posterior a la hora de entrega
    if (this.fechaLimiteDate === this.fechaEntregaDate) {
      const horaEnt = this.normalizarHora(this.horaEntregaTime);
      const horaLim = this.normalizarHora(this.horaLimiteTime);
      return horaLim <= horaEnt;
    }
    return false;
  }

  esEquipoInvalido(): boolean {
    if (!this.equipoSeleccionado) return true;
    // RN-02: Recursos bloqueados no se prestan
    if (this.equipoSeleccionado.estado === 'Bloqueado') return true;
    // RN-03: Recursos con estado Prestado no se prestan de nuevo
    if (this.equipoSeleccionado.estado === 'Prestado' || this.equipoSeleccionado.estado === 'En préstamo') return true;
    return false;
  }

  registrarEntrega(): void {
    this.errorModalEntrega = null;

    if (this.esEquipoInvalido()) {
      this.errorModalEntrega = 'El equipo seleccionado no está disponible para préstamo (Reglas RN-02 y RN-03).';
      this.cdr.detectChanges();
      return;
    }
    if (!this.usuarioSeleccionadoId || !this.entregaForm.usuarioResponsable?.trim()) {
      this.errorModalEntrega = 'Por favor selecciona un usuario responsable de la lista.';
      this.cdr.detectChanges();
      return;
    }
    if (!this.fechaEntregaDate || !this.horaEntregaTime) {
      this.errorModalEntrega = 'Por favor especifica la fecha y hora de entrega del equipo.';
      this.cdr.detectChanges();
      return;
    }
    if (!this.fechaLimiteDate || !this.horaLimiteTime) {
      this.errorModalEntrega = 'Por favor especifica la fecha y hora límite de devolución.';
      this.cdr.detectChanges();
      return;
    }
    if (this.esHorarioInvalido) {
      this.errorModalEntrega = 'La hora límite de devolución debe ser posterior a la hora de entrega.';
      this.cdr.detectChanges();
      return;
    }

    // Combina Fecha y Hora de Entrega en formato ISO LocalDateTime
    const horaEnt = this.normalizarHora(this.horaEntregaTime);
    this.entregaForm.fechaEntrega = `${this.fechaEntregaDate}T${horaEnt}:00`;

    // Combina Fecha y Hora Límite de Devolución en formato ISO LocalDateTime
    const horaLim = this.normalizarHora(this.horaLimiteTime);
    this.entregaForm.fechaLimite = `${this.fechaLimiteDate}T${horaLim}:00`;

    this.guardandoEntrega = true;
    this.entregaForm.recursoId = Number(this.entregaForm.recursoId);

    const idNum = Number(this.usuarioSeleccionadoId);
    this.entregaForm.idUsuario = !isNaN(idNum) && idNum > 0 ? idNum : this.usuarioSeleccionadoId;
    this.entregaForm.usuarioId = !isNaN(idNum) && idNum > 0 ? idNum : undefined;
    this.cdr.detectChanges();

    this.prestamoService.registrarEntrega(this.entregaForm).pipe(
      finalize(() => {
        this.guardandoEntrega = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (res) => {
        // Cierre inmediato del modal y forzado de detección de cambios en la vista
        this.cerrarModalEntrega();
        this.cargarPrestamos();
        this.cargarEquipos();
        const prestamoId = res?.idPrestamo || res?.id || '';
        const mensaje = prestamoId
          ? `¡Préstamo #${prestamoId} registrado exitosamente! El equipo pasó a estado "Prestado".`
          : '¡Préstamo registrado exitosamente! El equipo pasó a estado "Prestado".';
        this.mostrarMensajeExito(mensaje);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.guardandoEntrega = false;
        this.errorModalEntrega = err?.error?.message || err?.error?.error || err?.message || 'Error al registrar entrega';
        this.cdr.detectChanges();
      }
    });
  }

  // --- Modal Devolución ---
  abrirModalDevolucion(prestamo: any): void {
    this.prestamoParaDevolucion = prestamo;
    this.errorModalDevolucion = null;
    const ahora = new Date();
    this.fechaDevolucionDate = ahora.toISOString().split('T')[0];
    const horas = String(ahora.getHours()).padStart(2, '0');
    const minutos = String(ahora.getMinutes()).padStart(2, '0');
    this.horaDevolucionTime = `${horas}:${minutos}`;

    this.devolucionForm = {
      fechaDevolucion: '',
      estadoFinal: 'Operativo',
      observaciones: ''
    };
    this.cdr.detectChanges();
  }

  cerrarModalDevolucion(): void {
    this.prestamoParaDevolucion = null;
    this.guardandoDevolucion = false;
    this.errorModalDevolucion = null;
    this.cdr.detectChanges();
  }

  registrarDevolucion(): void {
    if (!this.prestamoParaDevolucion?.idPrestamo) return;
    this.errorModalDevolucion = null;

    // Combina Fecha y Hora de devolución
    const hora = this.normalizarHora(this.horaDevolucionTime || '12:00');
    this.devolucionForm.fechaDevolucion = `${this.fechaDevolucionDate}T${hora}:00`;

    this.guardandoDevolucion = true;
    this.cdr.detectChanges();

    this.prestamoService
      .registrarDevolucion(this.prestamoParaDevolucion.idPrestamo, this.devolucionForm)
      .pipe(
        finalize(() => {
          this.guardandoDevolucion = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (res) => {
          this.cerrarModalDevolucion();
          const puntualidad = res?.puntualidad || 'Puntual';
          this.cargarPrestamos();
          this.cargarEquipos();
          this.mostrarMensajeExito(
            `¡Devolución registrada! El equipo quedó "Disponible". Control de puntualidad: ${puntualidad}.`
          );
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.guardandoDevolucion = false;
          let msg = err?.error?.message || err?.error?.error || err?.message || 'Error al registrar devolución.';
          this.errorModalDevolucion = msg;
          this.cdr.detectChanges();
        }
      });
  }

  mostrarMensajeExito(msg: string): void {
    this.mensajeExito = msg;
    this.cdr.detectChanges();
    setTimeout(() => {
      this.mensajeExito = null;
      this.cdr.detectChanges();
    }, 6000);
  }
}
