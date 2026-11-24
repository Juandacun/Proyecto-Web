import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth';
import { NotificacionService } from '../services/notificacion';
import { LOGIN_URL, PERFIL_URL } from '../config/api';

function crearToken(): string {
  const b64 = (o: object) => btoa(JSON.stringify(o)).replace(/=+$/, '');
  return `${b64({ alg: 'HS256' })}.${b64({ sub: 'user', exp: Math.floor(Date.now() / 1000) + 3600 })}.firma`;
}

describe('authInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  let auth: AuthService;
  let router: Router;
  let notificaciones: NotificacionService;
  const token = crearToken();

  beforeEach(() => {
    sessionStorage.clear();
    sessionStorage.setItem('campusrecursos.token', token);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    notificaciones = TestBed.inject(NotificacionService);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => ctrl.verify());

  it('adjunta el JWT a las peticiones del backend', () => {
    http.get('http://localhost:8080/api/reservas').subscribe();
    const req = ctrl.expectOne('http://localhost:8080/api/reservas');
    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    req.flush([]);
  });

  it('no adjunta el JWT al login', () => {
    http.post(LOGIN_URL, {}).subscribe();
    const req = ctrl.expectOne(LOGIN_URL);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('no envía el JWT a dominios externos', () => {
    http.get('https://api.externa.com/datos').subscribe();
    const req = ctrl.expectOne('https://api.externa.com/datos');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('401 en una petición protegida: limpia la sesión y redirige al login', () => {
    http.get('http://localhost:8080/api/reservas').subscribe({ error: () => undefined });
    ctrl.expectOne('http://localhost:8080/api/reservas').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(auth.token()).toBeNull();
    expect(sessionStorage.getItem('campusrecursos.token')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], expect.objectContaining({
      queryParams: expect.objectContaining({ motivo: 'expirada' })
    }));
  });

  it('401 en el login: no redirige ni toca la sesión', () => {
    http.post(LOGIN_URL, {}).subscribe({ error: () => undefined });
    ctrl.expectOne(LOGIN_URL).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('401 en /me (verificación de sesión): limpia sin redirigir; la redirección la decide el guard', () => {
    auth.verificarSesion().subscribe();
    ctrl.expectOne(PERFIL_URL).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.token()).toBeNull();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('403: conserva el JWT y muestra acceso denegado', () => {
    http.delete('http://localhost:8080/api/usuarios/5').subscribe({ error: () => undefined });
    ctrl.expectOne('http://localhost:8080/api/usuarios/5').flush(
      { message: 'No posee los permisos suficientes.' },
      { status: 403, statusText: 'Forbidden' }
    );

    expect(auth.token()).toBe(token);
    expect(sessionStorage.getItem('campusrecursos.token')).toBe(token);
    expect(router.navigate).not.toHaveBeenCalled();
    expect(notificaciones.actual()?.texto).toContain('Acceso denegado');
  });
});
