import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FilaRecurso {
  idRecurso: number;
  recurso: string;
  categoria: string;
  ubicacion: string;
  clase: 'EQUIPO' | 'ESPACIO' | string;
  estado: string;
  reservas: number;
}

export interface FilaCategoria {
  categoria: string;
  reservas: number;
}

export interface Indicadores {
  totalRecursos: number;
  recursosPorEstado: Record<string, number>;
  reservasActivas: number;
  prestamosActivos: number;
  devolucionesTardias: number;
  incidentes: number;
  incidentesCriticos: number;
  reservasPorRecurso: FilaRecurso[];
  reservasPorCategoria: FilaCategoria[];
}

@Injectable({
  providedIn: 'root'
})
export class IndicadorService {
  private readonly apiUrl = 'http://localhost:8080/api/indicadores';

  constructor(private http: HttpClient) {}

  obtener(): Observable<Indicadores> {
    return this.http.get<Indicadores>(this.apiUrl);
  }
}
