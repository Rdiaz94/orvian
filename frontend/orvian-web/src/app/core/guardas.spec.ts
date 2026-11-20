// Utilidades de pruebas de Angular
import { TestBed } from '@angular/core/testing';
// Navegación
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
// Cliente HTTP de pruebas
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

// Utilidades de pruebas
import { USUARIO_PRUEBA, crearToken } from '../testing/utilidades';
// Guardas que se prueban
import { adminGuard, authGuard, invitadoGuard } from './guardas';

// Ejecuta una guarda dentro del contexto de Angular
function ejecutar(guarda: typeof authGuard): unknown {
  // Las guardas no usan los datos de la ruta en estas pruebas
  return TestBed.runInInjectionContext(() => guarda({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));
}

// Pruebas de las guardas de rutas
describe('guardas', () => {
  // Antes de cada prueba se limpia el almacenamiento y se configura Angular
  beforeEach(() => {
    // Borra cualquier sesión
    sessionStorage.clear();
    // Configura rutas y cliente HTTP
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
  });

  // Con sesión se entra
  it('authGuard deja pasar con una sesión vigente', () => {
    // Sesión vigente guardada
    sessionStorage.setItem('orvian.sesion', JSON.stringify({ token: crearToken(3600), usuario: USUARIO_PRUEBA }));
    // Entra
    expect(ejecutar(authGuard)).toBe(true);
  });

  // Sin sesión se va al login
  it('authGuard redirige al login sin sesión', () => {
    // Resultado de la guarda
    const resultado = ejecutar(authGuard);
    // Es una redirección
    expect(resultado).toBeInstanceOf(UrlTree);
    // Hacia el login
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/login');
  });

  // Con sesión vencida también se va al login
  it('authGuard redirige al login si el token ya expiró', () => {
    // Sesión vencida guardada
    sessionStorage.setItem('orvian.sesion', JSON.stringify({ token: crearToken(-60), usuario: USUARIO_PRUEBA }));
    // Resultado de la guarda
    const resultado = ejecutar(authGuard);
    // Es una redirección hacia el login
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/login');
  });

  // Quien ya tiene sesión no vuelve al login
  it('invitadoGuard redirige al inicio si ya hay sesión', () => {
    // Sesión vigente guardada
    sessionStorage.setItem('orvian.sesion', JSON.stringify({ token: crearToken(3600), usuario: USUARIO_PRUEBA }));
    // Resultado de la guarda
    const resultado = ejecutar(invitadoGuard);
    // Es una redirección hacia el inicio
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/inicio');
  });

  // Quien no tiene sesión puede ver el login
  it('invitadoGuard deja pasar sin sesión', () => {
    // Entra
    expect(ejecutar(invitadoGuard)).toBe(true);
  });

  // Solo los administradores entran a la administración
  it('adminGuard deja pasar a un administrador', () => {
    // Sesión de un administrador
    sessionStorage.setItem('orvian.sesion', JSON.stringify({ token: crearToken(3600, 'ADMIN'), usuario: { ...USUARIO_PRUEBA, rol: 'ADMIN' } }));
    // Entra
    expect(ejecutar(adminGuard)).toBe(true);
  });

  // Un estudiante vuelve al inicio
  it('adminGuard redirige a un estudiante al inicio', () => {
    // Sesión de un estudiante
    sessionStorage.setItem('orvian.sesion', JSON.stringify({ token: crearToken(3600), usuario: USUARIO_PRUEBA }));
    // Resultado de la guarda
    const resultado = ejecutar(adminGuard);
    // Es una redirección hacia el inicio
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/inicio');
  });

  // Sin sesión se va al login
  it('adminGuard redirige al login sin sesión', () => {
    // Resultado de la guarda
    const resultado = ejecutar(adminGuard);
    // Es una redirección hacia el login
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/login');
  });
});
