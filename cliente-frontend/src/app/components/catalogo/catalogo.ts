import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RecursoService } from '../../services/recurso';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalogo.html',
  styleUrls: ['./catalogo.css']
})
export class CatalogoComponent implements OnInit {
  recursos: any[] = [];
  filtroTexto: string = '';
  claseSeleccionada: string = '';
  estadoSeleccionado: string = '';

  constructor(private recursoService: RecursoService) {}

  ngOnInit(): void {
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
      return coincideTexto && coincideClase && coincideEstado;
    });
  }

  verDetalle(recurso: any) {
    alert(`Detalle del recurso:\nNombre: ${recurso.nombre}\nEstado: ${recurso.estado}`);
  }
}