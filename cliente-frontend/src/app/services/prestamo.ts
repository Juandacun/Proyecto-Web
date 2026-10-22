import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PrestamoEntregaDTO {
  recursoId: number;
  usuarioResponsable: string;
  idUsuario?: number | string;
  usuarioId?: number;
  fechaEntrega?: string;
  fechaLimite: string;
  estadoInicial?: string;
  observaciones?: string;
  accesorios?: string;
}


export interface PrestamoDevolucionDTO {
  fechaDevolucion?: string;
  estadoFinal?: string;
  observaciones?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PrestamoService {
  private readonly apiUrl = 'http://localhost:8080/api/prestamos';

  constructor(private http: HttpClient) {}

  listar(filtros?: { recursoId?: number; usuarioId?: string; activo?: boolean }): Observable<any[]> {
    let params = new HttpParams();
    if (filtros?.recursoId) {
      params = params.set('recursoId', filtros.recursoId.toString());
    }
    if (filtros?.usuarioId) {
      params = params.set('usuarioId', filtros.usuarioId);
    }
    if (filtros?.activo !== undefined) {
      params = params.set('activo', filtros.activo.toString());
    }
    return this.http.get<any[]>(this.apiUrl, { params });
  }

  listarActivos(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/activos`);
  }

  listarTardios(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/tardios`);
  }

  registrarEntrega(dto: PrestamoEntregaDTO): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/entrega`, dto);
  }

  registrarDevolucion(id: number, dto?: PrestamoDevolucionDTO): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/devolucion`, dto || {});
  }
}
