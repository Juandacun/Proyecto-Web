import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { Observable, firstValueFrom, isObservable, of } from 'rxjs';

import { roleGuard } from './role.guard';
import { authGuard } from './auth.guard';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { PERFIL_URL } from '../config/api';

function crearToken(): string {
  const b64 = (o: object) => btoa(JSON.stringify(o)).replace(/=+$/, '');
  return `${b64({ alg: 'HS256' })}.${b64({ sub: 'x', exp: Math.floor(Date.now() / 1000) + 3600 })}.firma`;
}

function ejecutar(guard: typeof roleGuard, roles: string[], url: string): Promise<boolean | UrlTree> {
  const route = { data: { roles } } as unknown as ActivatedRouteSnapshot;
  const state = { url } as RouterStateSnapshot;
  const resultado = TestBed.runInInjectionContext(() => guard(route, state));
  const obs = (isObservable(resultado) ? resultado : of(resultado)) as Observable<boolean | UrlTree>;
  return firstValueFrom(obs);
}

describe('Guards de autenticación y rol', () => {
  let ctrl: HttpTestingController;
  let router: Router;

  function configurar(conToken: boolean): void {
    sessionStorage.clear();
    if (conToken) sessionStorage.setItem('campusrecursos.token', crearToken());
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()]
    });
    ctrl = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  }

  afterEach(() => ctrl.verify());

  it('authGuard sin sesión redirige al login con returnUrl', async () => {
    configurar(false);
    const r = await ejecutar(authGuard, [], '/reservas');
    expect(router.serializeUrl(r as UrlTree)).toBe('/login?returnUrl=%2Freservas');
  });

  it('roleGuard permite a ROLE_ADMIN entrar al módulo admin', async () => {
    configurar(true);
    const promesa = ejecutar(roleGuard, ['ROLE_ADMIN'], '/admin/usuarios');
    ctrl.expectOne(PERFIL_URL).flush({ idUsuario: 1, nombre: 'Ana', username: 'admin', rol: 'ROLE_ADMIN' });
    expect(await promesa).toBe(true);
  });

  it('roleGuard envía a ROLE_USER a acceso denegado en el módulo admin', async () => {
    configurar(true);
    const promesa = ejecutar(roleGuard, ['ROLE_ADMIN'], '/admin/usuarios');
    ctrl.expectOne(PERFIL_URL).flush({ idUsuario: 2, nombre: 'Juan', username: 'user', rol: 'ROLE_USER' });
    const r = await promesa;
    expect(router.serializeUrl(r as UrlTree)).toBe('/acceso-denegado?desde=%2Fadmin%2Fusuarios');
  });

  it('roleGuard impide que ROLE_GESTOR administre usuarios pero le permite el tablero', async () => {
    configurar(true);
    const admin = ejecutar(roleGuard, ['ROLE_ADMIN'], '/admin/usuarios');
    ctrl.expectOne(PERFIL_URL).flush({ idUsuario: 3, nombre: 'Carlos', username: 'gestor', rol: 'ROLE_GESTOR' });
    expect(router.serializeUrl((await admin) as UrlTree)).toContain('/acceso-denegado');

    const tablero = ejecutar(roleGuard, ['ROLE_GESTOR', 'ROLE_ADMIN'], '/tablero');
    ctrl.expectOne(PERFIL_URL).flush({ idUsuario: 3, nombre: 'Carlos', username: 'gestor', rol: 'ROLE_GESTOR' });
    expect(await tablero).toBe(true);
  });
});
