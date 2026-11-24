import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api';
import { Rol } from '../services/auth';

/** Usuario tal como lo serializa el backend (la contraseña es WRITE_ONLY y nunca llega). */
export interface UsuarioAdmin {
  idUsuario: number;
  nombre: string;
  tipo: string;
  codigo: string;
  documento?: string | null;
  email?: string | null;
  username: string;
  rol: string;
}

/** Cuerpo para POST /api/usuarios. La contraseña viaja en texto plano: el backend aplica BCrypt. */
export interface UsuarioNuevo {
  nombre: string;
  tipo: string;
  codigo: string;
  documento?: string;
  email?: string;
  username?: string;
  password: string;
  rol: Rol;
}

// Incluye el rol actual: si se omite, el backend deja al usuario en ROLE_USER.
export interface UsuarioEdicion {
  nombre: string;
  tipo: string;
  codigo: string;
  documento: string;
  email: string;
  username: string;
  rol: string;
  password?: string;
}

/** Cliente de los endpoints administrativos de UsuarioController (exclusivos de ROLE_ADMIN). */
@Injectable({ providedIn: 'root' })
export class AdminUsuariosService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/api/usuarios`;

  listar(): Observable<UsuarioAdmin[]> {
    return this.http.get<UsuarioAdmin[]>(this.apiUrl);
  }

  obtener(id: number): Observable<UsuarioAdmin> {
    return this.http.get<UsuarioAdmin>(`${this.apiUrl}/${id}`);
  }

  crear(usuario: UsuarioNuevo): Observable<UsuarioAdmin> {
    return this.http.post<UsuarioAdmin>(this.apiUrl, usuario);
  }

  actualizar(id: number, datos: UsuarioEdicion): Observable<UsuarioAdmin> {
    return this.http.put<UsuarioAdmin>(`${this.apiUrl}/${id}`, datos);
  }

  cambiarRol(id: number, rol: Rol): Observable<UsuarioAdmin> {
    return this.http.patch<UsuarioAdmin>(`${this.apiUrl}/${id}/rol`, { rol });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
