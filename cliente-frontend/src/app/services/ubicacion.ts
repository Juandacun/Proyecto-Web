import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Ubicacion {
  idUbicacion: number;
  nombre: string;
  edificio?: string;
  descripcion?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UbicacionService {
  private readonly apiUrl = 'http://localhost:8080/api/ubicaciones';

  constructor(private http: HttpClient) {}

  listar(): Observable<Ubicacion[]> {
    return this.http.get<Ubicacion[]>(this.apiUrl);
  }
}
