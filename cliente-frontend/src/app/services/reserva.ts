import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type EstadoReserva = 'Confirmada' | 'Finalizada' | 'Cancelada';

export interface Reserva {
  idReserva: number;
  recurso: {
    idRecurso: number;
    nombre: string;
    tipo?: string;
    estado?: string;
    ubicacion?: { nombre: string };
    categoria?: { nombre: string };
  };
  fecha: string;
  horaInicio: string;
  horaFin: string;
  motivo?: string;
  estado: EstadoReserva | string;
  solicitante: string;
  editable?: boolean;
  iniciada?: boolean;
}

export interface ReservaDTO {
  recursoId: number;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  motivo?: string;
  solicitante?: string;
}

export interface Disponibilidad {
  disponible: boolean;
  motivo?: 'RN-01' | 'RN-02';
  mensaje?: string;
  error?: string;
  conflictoInicio?: string;
  conflictoFin?: string;
}

export interface FiltrosReserva {
  recursoId?: number | null;
  fecha?: string | null;
  estado?: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class ReservaService {
  private readonly apiUrl = 'http://localhost:8080/api/reservas';

  constructor(private http: HttpClient) {}

  listar(filtros?: FiltrosReserva): Observable<Reserva[]> {
    let params = new HttpParams();
    if (filtros?.recursoId) params = params.set('recursoId', String(filtros.recursoId));
    if (filtros?.fecha) params = params.set('fecha', filtros.fecha);
    if (filtros?.estado) params = params.set('estado', filtros.estado);
    return this.http.get<Reserva[]>(this.apiUrl, { params });
  }

  obtener(id: number): Observable<Reserva> {
    return this.http.get<Reserva>(`${this.apiUrl}/${id}`);
  }

  listarPorRecursoYFecha(recursoId: number, fecha: string): Observable<Reserva[]> {
    return this.listar({ recursoId, fecha });
  }

  verificarDisponibilidad(dto: ReservaDTO, reservaIdExcluir?: number | null): Observable<Disponibilidad> {
    let params = new HttpParams()
      .set('recursoId', String(dto.recursoId))
      .set('fecha', dto.fecha)
      .set('horaInicio', dto.horaInicio)
      .set('horaFin', dto.horaFin);
    if (reservaIdExcluir) params = params.set('reservaIdExcluir', String(reservaIdExcluir));
    return this.http.get<Disponibilidad>(`${this.apiUrl}/verificar-disponibilidad`, { params });
  }

  crear(dto: ReservaDTO): Observable<Reserva> {
    return this.http.post<Reserva>(this.apiUrl, dto);
  }

  actualizar(id: number, dto: ReservaDTO): Observable<Reserva> {
    return this.http.put<Reserva>(`${this.apiUrl}/${id}`, dto);
  }

  cancelar(id: number): Observable<Reserva> {
    return this.http.patch<Reserva>(`${this.apiUrl}/${id}/cancelar`, {});
  }

  finalizar(id: number): Observable<Reserva> {
    return this.http.patch<Reserva>(`${this.apiUrl}/${id}/finalizar`, {});
  }
}
