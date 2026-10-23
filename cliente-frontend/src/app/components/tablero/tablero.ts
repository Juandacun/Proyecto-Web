import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IndicadorService, Indicadores, FilaRecurso } from '../../services/indicador';

@Component({
  selector: 'app-tablero',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tablero.html',
  styleUrls: ['./tablero.css']
})
export class TableroComponent implements OnInit {
  indicadores: Indicadores | null = null;
  cargando = false;
  mensajeError: string | null = null;

  // Filtro de la tabla de estado actual (4.f.i)
  estadoSeleccionado = '';
  readonly estados = ['Disponible', 'Reservado', 'Prestado', 'Bloqueado'];

  constructor(private indicadorService: IndicadorService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.mensajeError = null;
    this.indicadorService.obtener().subscribe({
      next: (data) => {
        this.indicadores = data;
        this.cargando = false;
      },
      error: (err) => {
        this.cargando = false;
        this.mensajeError = err?.error?.error || 'No se pudieron cargar los indicadores. Verifica que el servidor esté activo.';
      }
    });
  }

  get filasFiltradas(): FilaRecurso[] {
    const filas = this.indicadores?.reservasPorRecurso ?? [];
    if (!this.estadoSeleccionado) return filas;
    return filas.filter((f) => this.estadoTablero(f.estado) === this.estadoSeleccionado);
  }

  get maxReservasCategoria(): number {
    const filas = this.indicadores?.reservasPorCategoria ?? [];
    return Math.max(1, ...filas.map((f) => f.reservas));
  }

  porcentaje(valor: number): number {
    return Math.round((valor / this.maxReservasCategoria) * 100);
  }

  conteoEstado(estado: string): number {
    return this.indicadores?.recursosPorEstado?.[estado] ?? 0;
  }

  estadoTablero(estado: string): string {
    const e = (estado || '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
    if (e.includes('bloque') || e.includes('mantenimiento')) return 'Bloqueado';
    if (e.includes('prest')) return 'Prestado';
    if (e.includes('reserv')) return 'Reservado';
    if (e.includes('disponible')) return 'Disponible';
    return estado;
  }

  claseTag(estado: string): string {
    switch (this.estadoTablero(estado)) {
      case 'Disponible': return 'tag--verde';
      case 'Reservado': return 'tag--azul';
      case 'Prestado': return 'tag--ambar';
      case 'Bloqueado': return 'tag--rojo';
      default: return 'tag--gris';
    }
  }
}
