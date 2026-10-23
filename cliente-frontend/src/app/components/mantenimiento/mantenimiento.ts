import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IncidenteService, IncidenteDTO } from '../../services/incidente';
import { MantenimientoService, MantenimientoDTO } from '../../services/mantenimiento';
import { RecursoService } from '../../services/recurso';

@Component({
  selector: 'app-mantenimiento',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mantenimiento.html',
  styleUrls: ['./mantenimiento.css']
})
export class MantenimientoComponent implements OnInit {
  mantenimientos: any[] = [];
  incidentes: any[] = [];
  recursos: any[] = [];

  cargando = false;
  mensajeExito: string | null = null;
  mensajeError: string | null = null;

  // Formulario Reportar Incidente (RN-04)
  incidenteForm: IncidenteDTO = {
    recursoId: 0,
    usuarioId: 'DOC-102938',
    reportadoPor: 'Estudiante / Docente',
    tipoNovedad: 'Falla de funcionamiento',
    severidad: 'Media',
    descripcion: ''
  };
  recursoSeleccionadoParaIncidente: any = null;
  guardandoIncidente = false;

  // Modal / Form Crear Orden Mantenimiento
  mostrarModalNuevaOrden = false;
  mantenimientoForm: MantenimientoDTO = {
    recursoId: 0,
    diagnostico: '',
    idUsuarioResponsable: 'TÉCNICO-CAMPUS',
    observaciones: ''
  };
  guardandoOrden = false;

  constructor(
    private incidenteService: IncidenteService,
    private mantenimientoService: MantenimientoService,
    private recursoService: RecursoService
  ) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando = true;
    this.mensajeError = null;

    // Cargar recursos
    this.recursoService.listar().subscribe({
      next: (data) => (this.recursos = data),
      error: (err) => console.error('Error cargando recursos', err)
    });

    // Cargar órdenes de mantenimiento
    this.cargarMantenimientos();

    // Cargar incidentes
    this.incidenteService.listar().subscribe({
      next: (data) => (this.incidentes = data),
      error: (err) => console.error('Error cargando incidentes', err)
    });
  }

  cargarMantenimientos(): void {
    this.mantenimientoService.listar().subscribe({
      next: (data) => {
        this.mantenimientos = data;
        this.cargando = false;
      },
      error: (err) => {
        console.error(err);
        this.mensajeError = 'Error al cargar órdenes de mantenimiento.';
        this.cargando = false;
      }
    });
  }

  onSeleccionarRecursoIncidente(): void {
    const id = Number(this.incidenteForm.recursoId);
    this.recursoSeleccionadoParaIncidente = this.recursos.find((r) => r.idRecurso === id) || null;
  }

  // --- Reporte de Incidente (RN-04) ---
 reportarIncidente(): void {
  this.mensajeError = null;

  if (!this.incidenteForm.recursoId || Number(this.incidenteForm.recursoId) === 0) {
    this.mensajeError = 'Selecciona el recurso afectado.';
    return;
  }
  if (!this.incidenteForm.descripcion?.trim()) {
    this.mensajeError = 'Escribe la descripción del incidente.';
    return;
  }
  if (this.incidenteForm.descripcion.length > 255) {
    this.mensajeError = 'La descripción no puede superar 255 caracteres.';
    return;
  }

  this.guardandoIncidente = true;
  this.incidenteForm.recursoId = Number(this.incidenteForm.recursoId);

  this.incidenteService.registrar(this.incidenteForm).subscribe({
    next: (res) => {
      this.guardandoIncidente = false;
      const nombre = this.recursoSeleccionadoParaIncidente?.nombre;

      if (res?.recurso?.estado === 'Bloqueado') {
        this.mostrarMensajeExito(
          `Incidente crítico reportado. El recurso "${nombre}" quedó Bloqueado (RN-04).`
        );
      } else {
        this.mostrarMensajeExito(
          `Incidente de severidad ${res?.severidad ?? this.incidenteForm.severidad} registrado correctamente.`
        );
      }

      this.incidenteForm.descripcion = '';
      this.incidenteForm.recursoId = 0;
      this.recursoSeleccionadoParaIncidente = null;
      this.cargarDatos();
    },
    error: (err) => {
      this.guardandoIncidente = false;
      this.mensajeError =
        err.error?.error || 'No se pudo registrar el incidente. Intenta de nuevo.';
    }
  });
}
  // --- Finalizar Mantenimiento (RN-05) ---
  finalizarMantenimiento(mantenimiento: any): void {
    if (!confirm(`¿Deseas finalizar la orden de mantenimiento #${mantenimiento.idMantenimiento}? El recurso volverá automáticamente al estado "Disponible" (RN-05).`)) {
      return;
    }

    this.mantenimientoService.finalizar(mantenimiento.idMantenimiento).subscribe({
      next: (res) => {
        const nombreRecurso = mantenimiento.recurso?.nombre || 'el recurso';
        this.mostrarMensajeExito(
          `¡Orden #${mantenimiento.idMantenimiento} finalizada (RN-05)! "${nombreRecurso}" fue liberado y ahora está en estado "Disponible".`
        );
        this.cargarDatos();
      },
      error: (err) => {
        alert(`Error al finalizar mantenimiento: ${err.error?.error || err.message}`);
      }
    });
  }

  // --- Cambiar estado de la intervención (Abierta -> En proceso / Cancelada) ---
  iniciarMantenimiento(mantenimiento: any): void {
    this.mantenimientoService.cambiarEstado(mantenimiento.idMantenimiento, 'En proceso').subscribe({
      next: () => {
        this.mostrarMensajeExito(`Orden #${mantenimiento.idMantenimiento} en proceso.`);
        this.cargarDatos();
      },
      error: (err) => {
        this.mensajeError = err.error?.error || 'No se pudo cambiar el estado de la orden.';
      }
    });
  }

  cancelarMantenimiento(mantenimiento: any): void {
    if (!confirm(`¿Deseas cancelar la orden de mantenimiento #${mantenimiento.idMantenimiento}? El recurso seguirá bloqueado hasta que se habilite.`)) {
      return;
    }
    this.mantenimientoService.cambiarEstado(mantenimiento.idMantenimiento, 'Cancelada').subscribe({
      next: () => {
        this.mostrarMensajeExito(`Orden #${mantenimiento.idMantenimiento} cancelada. El recurso permanece bloqueado.`);
        this.cargarDatos();
      },
      error: (err) => {
        this.mensajeError = err.error?.error || 'No se pudo cancelar la orden.';
      }
    });
  }

  estaCerrada(mantenimiento: any): boolean {
    return mantenimiento.estado === 'Finalizada' || mantenimiento.estado === 'Cancelada';
  }

  // --- Crear Orden de Mantenimiento ---
  abrirModalNuevaOrden(): void {
    this.mostrarModalNuevaOrden = true;
    this.mantenimientoForm = {
      recursoId: 0,
      diagnostico: '',
      idUsuarioResponsable: 'TÉCNICO-CAMPUS',
      observaciones: ''
    };
  }

  cerrarModalNuevaOrden(): void {
    this.mostrarModalNuevaOrden = false;
  }

  guardarNuevaOrden(): void {
    if (!this.mantenimientoForm.recursoId || this.mantenimientoForm.recursoId === 0) {
      alert('Por favor selecciona un recurso para intervenir.');
      return;
    }
    if (!this.mantenimientoForm.diagnostico?.trim()) {
      alert('Por favor ingresa el diagnóstico técnico.');
      return;
    }

    this.guardandoOrden = true;
    this.mantenimientoForm.recursoId = Number(this.mantenimientoForm.recursoId);

    this.mantenimientoService.crearOrden(this.mantenimientoForm).subscribe({
      next: (res) => {
        this.guardandoOrden = false;
        this.cerrarModalNuevaOrden();
        this.mostrarMensajeExito(`Orden de mantenimiento #${res.idMantenimiento} creada con éxito.`);
        this.cargarDatos();
      },
      error: (err) => {
        this.guardandoOrden = false;
        alert(`Error al crear orden: ${err.error?.error || err.message}`);
      }
    });
  }

  mostrarMensajeExito(msg: string): void {
    this.mensajeExito = msg;
    setTimeout(() => (this.mensajeExito = null), 7000);
  }
}
