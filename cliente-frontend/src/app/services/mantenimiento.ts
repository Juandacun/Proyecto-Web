import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MantenimientoDTO {
  recursoId: number;
  fechaApertura?: string;
  diagnostico?: string;
  estado?: string;
  fechaCierre?: string;
  idUsuarioResponsable?: string;
  observaciones?: string;
}

@Injectable({
  providedIn: 'root'
})
export class MantenimientoService {
  private readonly apiUrl = 'http://localhost:8080/api/mantenimientos';

  constructor(private http: HttpClient) {}

  listar(filtros?: { recursoId?: number; estado?: string }): Observable<any[]> {
    let params = new HttpParams();
    if (filtros?.recursoId) {
      params = params.set('recursoId', filtros.recursoId.toString());
    }
    if (filtros?.estado) {
      params = params.set('estado', filtros.estado);
    }
    return this.http.get<any[]>(this.apiUrl, { params });
  }

  crearOrden(dto: MantenimientoDTO): Observable<any> {
    return this.http.post<any>(this.apiUrl, dto);
  }

  finalizar(id: number): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/finalizar`, {});
  }
}
