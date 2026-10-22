import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
export class PrestamosComponent implements OnInit {
  prestamos: any[] = [];
  equiposDisponibles: any[] = [];
  usuarios: Usuario[] = [];

  tabSeleccionada: 'todos' | 'activos' | 'tardios' = 'activos';

  cargando = false;
  mensajeExito: string | null = null;
  mensajeError: string | null = null;

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
    private usuarioService: UsuarioService
  ) {}

  ngOnInit(): void {
    this.cargarPrestamos();
    this.cargarEquipos();
    this.cargarUsuarios();
  }

  cargarPrestamos(): void {
    this.cargando = true;
    this.mensajeError = null;

    if (this.tabSeleccionada === 'activos') {
      this.prestamoService.listarActivos().subscribe({
        next: (data) => {
          this.prestamos = data;
          this.cargando = false;
        },
        error: (err) => {
          console.error(err);
          this.mensajeError = 'Error al cargar préstamos activos.';
          this.cargando = false;
        }
      });
    } else if (this.tabSeleccionada === 'tardios') {
      this.prestamoService.listarTardios().subscribe({
        next: (data) => {
          this.prestamos = data;
          this.cargando = false;
        },
        error: (err) => {
          console.error(err);
          this.mensajeError = 'Error al cargar préstamos tardíos.';
          this.cargando = false;
        }
      });
    } else {
      this.prestamoService.listar().subscribe({
        next: (data) => {
          this.prestamos = data;
          this.cargando = false;
        },
        error: (err) => {
          console.error(err);
          this.mensajeError = 'Error al cargar historial de préstamos.';
          this.cargando = false;
        }
      });
    }
  }

  cargarEquipos(): void {
    this.recursoService.listarEquipos().subscribe({
      next: (data) => {
        this.equiposDisponibles = data;
      },
      error: (err) => console.error('Error al cargar equipos', err)
    });
  }

  cargarUsuarios(): void {
    this.usuarioService.listar().subscribe({
      next: (data) => {
        this.usuarios = data;
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
      }
    });
  }

  cambiarTab(tab: 'todos' | 'activos' | 'tardios'): void {
    this.tabSeleccionada = tab;
    this.cargarPrestamos();
  }

  // --- Modal Entrega (RN-02, RN-03) ---
  abrirModalEntrega(): void {
    this.cargarEquipos();
    this.cargarUsuarios();
    this.mostrarModalEntrega = true;
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
      fechaEntrega: '',
      fechaLimite: '',
      estadoInicial: 'Operativo',
      observaciones: '',
      accesorios: 'Cables, cargador, estuche'
    };
  }

  cerrarModalEntrega(): void {
    this.mostrarModalEntrega = false;
    this.equipoSeleccionado = null;
    this.usuarioSeleccionadoId = '';
    this.errorModalEntrega = null;
  }

  onSeleccionarEquipo(): void {
    this.errorModalEntrega = null;
    const id = Number(this.entregaForm.recursoId);
    this.equipoSeleccionado = this.equiposDisponibles.find((e) => e.idRecurso === id) || null;
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
      return;
    }
    if (!this.usuarioSeleccionadoId || !this.entregaForm.usuarioResponsable?.trim()) {
      this.errorModalEntrega = 'Por favor selecciona un usuario responsable de la lista.';
      return;
    }
    if (!this.fechaEntregaDate || !this.horaEntregaTime) {
      this.errorModalEntrega = 'Por favor especifica la fecha y hora de entrega del equipo.';
      return;
    }
    if (!this.fechaLimiteDate || !this.horaLimiteTime) {
      this.errorModalEntrega = 'Por favor especifica la fecha y hora límite de devolución.';
      return;
    }
    if (this.esHorarioInvalido) {
      this.errorModalEntrega = 'La hora límite de devolución debe ser posterior a la hora de entrega.';
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

    this.prestamoService.registrarEntrega(this.entregaForm).subscribe({
      next: (res) => {
        this.guardandoEntrega = false;
        this.cerrarModalEntrega();
        this.mostrarMensajeExito(`¡Préstamo #${res.idPrestamo} registrado exitosamente! El equipo pasó a estado "Prestado".`);
        this.cargarPrestamos();
        this.cargarEquipos();
      },
      error: (err) => {
        this.guardandoEntrega = false;
        let msg = 'Error al registrar entrega.';
        if (err.error) {
          if (typeof err.error === 'string') {
            msg = err.error;
          } else if (err.error.message && typeof err.error.message === 'string') {
            msg = err.error.message;
          } else if (err.error.error && typeof err.error.error === 'string') {
            msg = err.error.error;
          }
        } else if (err.message) {
          msg = err.message;
        }
        this.errorModalEntrega = msg;
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
  }

  cerrarModalDevolucion(): void {
    this.prestamoParaDevolucion = null;
    this.errorModalDevolucion = null;
  }

  registrarDevolucion(): void {
    if (!this.prestamoParaDevolucion?.idPrestamo) return;
    this.errorModalDevolucion = null;

    // Combina Fecha y Hora de devolución
    const hora = this.normalizarHora(this.horaDevolucionTime || '12:00');
    this.devolucionForm.fechaDevolucion = `${this.fechaDevolucionDate}T${hora}:00`;

    this.guardandoDevolucion = true;
    this.prestamoService
      .registrarDevolucion(this.prestamoParaDevolucion.idPrestamo, this.devolucionForm)
      .subscribe({
        next: (res) => {
          this.guardandoDevolucion = false;
          const puntualidad = res.puntualidad || 'Puntual';
          this.cerrarModalDevolucion();
          this.mostrarMensajeExito(
            `¡Devolución registrada! El equipo quedó "Disponible". Control de puntualidad: ${puntualidad}.`
          );
          this.cargarPrestamos();
          this.cargarEquipos();
        },
        error: (err) => {
          this.guardandoDevolucion = false;
          let msg = 'Error al registrar devolución.';
          if (err.error) {
            if (typeof err.error === 'string') {
              msg = err.error;
            } else if (err.error.message && typeof err.error.message === 'string') {
              msg = err.error.message;
            } else if (err.error.error && typeof err.error.error === 'string') {
              msg = err.error.error;
            }
          } else if (err.message) {
            msg = err.message;
          }
          this.errorModalDevolucion = msg;
        }
      });
  }

  mostrarMensajeExito(msg: string): void {
    this.mensajeExito = msg;
    setTimeout(() => (this.mensajeExito = null), 6000);
  }
}
