import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Usuario {
  idUsuario: number;
  nombre: string;
  tipo: string;
  codigo: string;
  documento?: string;
  email?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {
  private readonly apiUrl = 'http://localhost:8080/api/usuarios';

  constructor(private http: HttpClient) {}

  listar(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(this.apiUrl);
  }

  obtenerPorId(id: number): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.apiUrl}/${id}`);
  }
}
