import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
    providedIn: 'root'
})

export class RecursoService {
    private apiUrl = 'http://localhost:8080/api/recursos'; 
    constructor(private http: HttpClient) {}
    
    obtenerRecursos(filtros?: {clase?: string, estado?: string, disponible?: boolean,
                               categoriaId?: number, ubicacionId?: number, tipo?: string}): Observable<any[]> {
        let params = new HttpParams();
        if (filtros) {
            if (filtros.clase) params = params.set('clase', filtros.clase);
            if (filtros.estado) params = params.set('estado', filtros.estado);
            if (filtros.disponible !== undefined) params = params.set('disponible', filtros.disponible);
            if (filtros.categoriaId) params = params.set('categoriaId', filtros.categoriaId);
            if (filtros.ubicacionId) params = params.set('ubicacionId', filtros.ubicacionId);
            if (filtros.tipo) params = params.set('tipo', filtros.tipo);
        }
        return this.http.get<any[]>(this.apiUrl, { params });
    }

    listar(): Observable<any[]> {
        return this.http.get<any[]>(this.apiUrl);
    }

    listarEquipos(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/equipos`);
    }
}


