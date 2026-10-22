import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface IncidenteDTO {
  recursoId: number;
  usuarioId?: string;
  reportadoPor?: string;
  tipoNovedad: string;
  severidad: 'Leve' | 'Media' | 'Crítica' | string;
  descripcion: string;
  fecha?: string;
}

@Injectable({
  providedIn: 'root'
})
export class IncidenteService {
  private readonly apiUrl = 'http://localhost:8080/api/incidentes';

  constructor(private http: HttpClient) {}

  listar(filtros?: { recursoId?: number; severidad?: string }): Observable<any[]> {
    let params = new HttpParams();
    if (filtros?.recursoId) {
      params = params.set('recursoId', filtros.recursoId.toString());
    }
    if (filtros?.severidad) {
      params = params.set('severidad', filtros.severidad);
    }
    return this.http.get<any[]>(this.apiUrl, { params });
  }

  registrar(dto: IncidenteDTO): Observable<any> {
    return this.http.post<any>(this.apiUrl, dto);
  }
}
