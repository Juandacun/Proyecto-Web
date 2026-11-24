import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { AuthService, normalizarRol } from './auth';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { LOGIN_URL, PERFIL_URL } from '../config/api';

/** JWT de prueba (sin firma válida; la SPA solo lee "exp"). */
function crearToken(expEnSegundos: number): string {
  const b64 = (o: object) => btoa(JSON.stringify(o)).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');
  return `${b64({ alg: 'HS256' })}.${b64({ sub: 'admin', exp: expEnSegundos })}.firma`;
}

const PERFIL_ADMIN = {
  idUsuario: 3,
  nombre: 'Ing. Ana Torres',
  tipo: 'Administrativo',
  codigo: 'ADM-401928',
  username: 'admin',
  rol: 'ROLE_ADMIN'
};

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  const tokenVigente = crearToken(Math.floor(Date.now() / 1000) + 3600);

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('inicia sesión: toma el JWT de la cabecera Authorization y carga el rol desde /me', () => {
    let nombre = '';
    auth.login('admin', 'admin123').subscribe((u) => (nombre = u.nombre));

    const login = http.expectOne(LOGIN_URL);
    expect(login.request.method).toBe('POST');
    expect(login.request.body).toEqual({ username: 'admin', password: 'admin123' });
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush({ rol: 'ROLE_USER' }, { headers: { Authorization: `Bearer ${tokenVigente}` } });

    const me = http.expectOne(PERFIL_URL);
    expect(me.request.headers.get('Authorization')).toBe(`Bearer ${tokenVigente}`);
    me.flush(PERFIL_ADMIN);

    expect(nombre).toBe('Ing. Ana Torres');
    expect(auth.estaAutenticado()).toBe(true);
    // El rol vigente es el de /me, no el del cuerpo del login.
    expect(auth.esAdmin()).toBe(true);
    expect(sessionStorage.getItem('campusrecursos.token')).toBe(tokenVigente);
  });

  it('nunca guarda la contraseña ni el rol en sessionStorage', () => {
    auth.login('admin', 'admin123').subscribe();
    http.expectOne(LOGIN_URL).flush({}, { headers: { Authorization: `Bearer ${tokenVigente}` } });
    http.expectOne(PERFIL_URL).flush(PERFIL_ADMIN);

    const guardado = Object.keys(sessionStorage).map((k) => sessionStorage.getItem(k)).join('|');
    expect(guardado).not.toContain('admin123');
    expect(guardado).not.toContain('ROLE_ADMIN');
  });

  it('credenciales incorrectas (401 en login): no guarda sesión', () => {
    let status = 0;
    auth.login('admin', 'mala').subscribe({ error: (e) => (status = e.status) });
    http.expectOne(LOGIN_URL).flush({ message: 'Nombre de usuario o contraseña incorrectos.' }, { status: 401, statusText: 'Unauthorized' });

    expect(status).toBe(401);
    expect(auth.estaAutenticado()).toBe(false);
    expect(auth.token()).toBeNull();
  });

  it('recupera la sesión al recargar validándola contra /me', () => {
    sessionStorage.setItem('campusrecursos.token', tokenVigente);
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()]
    });
    const nuevo = TestBed.inject(AuthService);
    const ctrl = TestBed.inject(HttpTestingController);

    let valida: boolean | undefined;
    nuevo.verificarSesion().subscribe((v) => (valida = v));
    ctrl.expectOne(PERFIL_URL).flush({ ...PERFIL_ADMIN, rol: 'ROLE_GESTOR' });

    expect(valida).toBe(true);
    expect(nuevo.esGestor()).toBe(true);
    ctrl.verify();
  });

  it('descarta la sesión si /me responde 401', () => {
    sessionStorage.setItem('campusrecursos.token', tokenVigente);
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()]
    });
    const nuevo = TestBed.inject(AuthService);
    const ctrl = TestBed.inject(HttpTestingController);

    let valida: boolean | undefined;
    nuevo.verificarSesion().subscribe((v) => (valida = v));
    ctrl.expectOne(PERFIL_URL).flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(valida).toBe(false);
    expect(nuevo.token()).toBeNull();
    expect(sessionStorage.getItem('campusrecursos.token')).toBeNull();
    ctrl.verify();
  });

  it('no consulta /me con un token vencido', () => {
    sessionStorage.setItem('campusrecursos.token', crearToken(Math.floor(Date.now() / 1000) - 10));
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()]
    });
    const nuevo = TestBed.inject(AuthService);
    const ctrl = TestBed.inject(HttpTestingController);

    let valida: boolean | undefined;
    nuevo.verificarSesion().subscribe((v) => (valida = v));
    ctrl.expectNone(PERFIL_URL);
    expect(valida).toBe(false);
    ctrl.verify();
  });

  it('normaliza solo los tres roles válidos', () => {
    expect(normalizarRol('admin')).toBe('ROLE_ADMIN');
    expect(normalizarRol('ROLE_GESTOR')).toBe('ROLE_GESTOR');
    expect(normalizarRol('ROLE_SUPERADMIN')).toBeNull();
    expect(normalizarRol(null)).toBeNull();
  });
});
