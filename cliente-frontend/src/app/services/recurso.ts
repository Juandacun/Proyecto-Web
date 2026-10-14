import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, Service } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';

@Injectable({
    providedIn: 'root'
})

export class RecursoService {
    private apiUrl = 'http://localhost:8080/api/recursos'; 
    constructor(private http: HttpClient) {}
    
    obtenerRecursos(filtros?: {clase?: string, estado?: string, disponible?: boolean}): Observable<any[]> {
        let params = new HttpParams();
        if (filtros) {
            if (filtros.clase) params = params.set('clase', filtros.clase);
            if (filtros.estado) params = params.set('estado', filtros.estado);
            if (filtros.disponible !== undefined) params = params.set('disponible', filtros.disponible);
        }
        return this.http.get<any[]>(this.apiUrl, { params });
    }
}

