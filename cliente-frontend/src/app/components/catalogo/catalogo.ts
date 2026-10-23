import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { RecursoService } from '../../services/recurso';
import { CategoriaService, Categoria } from '../../services/categoria';
import { UbicacionService, Ubicacion } from '../../services/ubicacion';
import { ReservaService, Reserva } from '../../services/reserva';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './catalogo.html',
  styleUrls: ['./catalogo.css']
})
export class CatalogoComponent implements OnInit {
  recursos: any[] = [];
  categorias: Categoria[] = [];
  ubicaciones: Ubicacion[] = [];
  tipos: string[] = [];

  filtroTexto: string = '';
  claseSeleccionada: string = '';
  estadoSeleccionado: string = '';
  categoriaSeleccionada: number | null = null;
  ubicacionSeleccionada: number | null = null;
  tipoSeleccionado: string = '';

  // Panel de detalle y disponibilidad básica
  recursoDetalle: any = null;
  proximasReservas: Reserva[] = [];
  cargandoReservas = false;

  constructor(
    private recursoService: RecursoService,
    private categoriaService: CategoriaService,
    private ubicacionService: UbicacionService,
    private reservaService: ReservaService
  ) {}

  ngOnInit(): void {
    this.categoriaService.listar().subscribe({ next: (data) => (this.categorias = data) });
    this.ubicacionService.listar().subscribe({ next: (data) => (this.ubicaciones = data) });
    // Los tipos se toman del catálogo completo para que no desaparezcan al filtrar
    this.recursoService.listar().subscribe({
      next: (data) => {
        this.tipos = [...new Set(data.map((r) => r.tipo).filter((t) => !!t))].sort();
      }
    });
    this.cargarRecursos();
  }

  cargarRecursos() {
    const filtros: any = {};
    if (this.claseSeleccionada) {
      filtros.clase = this.claseSeleccionada.toUpperCase();
    }
    if (this.estadoSeleccionado) {
      filtros.estado = this.estadoSeleccionado;
    }
    if (this.categoriaSeleccionada) {
      filtros.categoriaId = Number(this.categoriaSeleccionada);
    }
    if (this.ubicacionSeleccionada) {
      filtros.ubicacionId = Number(this.ubicacionSeleccionada);
    }
    if (this.tipoSeleccionado) {
      filtros.tipo = this.tipoSeleccionado;
    }

    this.recursoService.obtenerRecursos(filtros).subscribe({
      next: (data) => {
        this.recursos = data;
      }
    });
  }
  aplicarFiltros() {
    this.cargarRecursos();
  }

  limpiarFiltros() {
    this.filtroTexto = '';
    this.claseSeleccionada = '';
    this.estadoSeleccionado = '';
    this.categoriaSeleccionada = null;
    this.ubicacionSeleccionada = null;
    this.tipoSeleccionado = '';
    this.cargarRecursos();
  }

  get recursosFiltrados() {
    if (!this.recursos || this.recursos.length === 0) return [];
    return this.recursos.filter(recurso => {
      const texto = this.filtroTexto.toLowerCase();
      const coincideTexto = !this.filtroTexto ||
        (recurso.nombre && recurso.nombre.toLowerCase().includes(texto)) ||
        (recurso.tipo && recurso.tipo.toLowerCase().includes(texto)) ||
        (recurso.categoria?.nombre && recurso.categoria.nombre.toLowerCase().includes(texto)) ||
        (recurso.ubicacion?.nombre && recurso.ubicacion.nombre.toLowerCase().includes(texto));
      const coincideClase = !this.claseSeleccionada || recurso.clase === this.claseSeleccionada.toUpperCase();
      const coincideEstado = !this.estadoSeleccionado || recurso.estado === this.estadoSeleccionado;
      const coincideCategoria = !this.categoriaSeleccionada
        || recurso.categoria?.idCategoria === Number(this.categoriaSeleccionada);
      const coincideUbicacion = !this.ubicacionSeleccionada
        || recurso.ubicacion?.idUbicacion === Number(this.ubicacionSeleccionada);
      const coincideTipo = !this.tipoSeleccionado || recurso.tipo === this.tipoSeleccionado;
      return coincideTexto && coincideClase && coincideEstado
        && coincideCategoria && coincideUbicacion && coincideTipo;
    });
  }

  verDetalle(recurso: any) {
    this.recursoDetalle = recurso;
    this.proximasReservas = [];
    if (recurso.clase !== 'ESPACIO') return;

    this.cargandoReservas = true;
    const hoy = new Date().toISOString().slice(0, 10);
    this.reservaService.listar({ recursoId: recurso.idRecurso, estado: 'Confirmada' }).subscribe({
      next: (data) => {
        this.proximasReservas = data.filter((r) => r.fecha >= hoy);
        this.cargandoReservas = false;
      },
      error: () => (this.cargandoReservas = false)
    });
  }

  cerrarDetalle() {
    this.recursoDetalle = null;
  }
}
