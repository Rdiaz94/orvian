// Utilidades de pruebas de Angular
import { TestBed } from '@angular/core/testing';
// Cliente HTTP y su versión de pruebas
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

// Dirección de la API
import { environment } from '../../environments/environment';
// Utilidades de pruebas
import { USUARIO_PRUEBA, crearToken } from '../testing/utilidades';
// Servicio que se prueba
import { AuthService } from './auth.service';

// Clave con la que el servicio guarda la sesión
const CLAVE = 'orvian.sesion';

// Pruebas del servicio de sesión
describe('AuthService', () => {
  // Antes de cada prueba se limpia el almacenamiento y se configura Angular
  beforeEach(() => {
    // Borra cualquier sesión de pruebas anteriores
    sessionStorage.clear();
    // Configura el cliente HTTP de pruebas
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
  });

  // El inicio de sesión guarda el token
  it('guarda la sesión al iniciar sesión', () => {
    // Servicio y controlador de peticiones simuladas
    const auth = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);
    // Token vigente que devolverá la API
    const token = crearToken(3600);

    // Inicia sesión
    auth.login({ correo: 'ana@correo.com', contrasena: 'Clave-123456' }).subscribe();
    // La petición va a la ruta de login
    const peticion = http.expectOne(`${environment.apiUrl}/api/auth/login`);
    // Y es de tipo POST
    expect(peticion.request.method).toBe('POST');
    // La API responde con el token
    peticion.flush({ token, tipo: 'Bearer', expiraEnSegundos: 3600, usuario: USUARIO_PRUEBA });

    // Hay sesión
    expect(auth.autenticado()).toBe(true);
    // El token quedó guardado
    expect(auth.token()).toBe(token);
    // Y también en el almacenamiento de la pestaña
    expect(sessionStorage.getItem(CLAVE)).not.toBeNull();
  });

  // Cerrar sesión borra todo
  it('borra la sesión al cerrarla', () => {
    // Deja una sesión vigente guardada
    sessionStorage.setItem(CLAVE, JSON.stringify({ token: crearToken(3600), usuario: USUARIO_PRUEBA }));
    // Servicio, que restaura la sesión
    const auth = TestBed.inject(AuthService);
    // Hay sesión
    expect(auth.autenticado()).toBe(true);

    // Cierra la sesión
    auth.cerrarSesion();

    // Ya no hay sesión
    expect(auth.autenticado()).toBe(false);
    // Y no queda nada guardado
    expect(sessionStorage.getItem(CLAVE)).toBeNull();
  });

  // Al recargar la página se recupera la sesión
  it('restaura una sesión vigente guardada', () => {
    // Deja una sesión vigente guardada
    sessionStorage.setItem(CLAVE, JSON.stringify({ token: crearToken(3600), usuario: USUARIO_PRUEBA }));
    // Servicio, que restaura la sesión
    const auth = TestBed.inject(AuthService);
    // La sesión se recuperó
    expect(auth.usuario()?.nombre).toBe('Ana Pérez');
  });

  // Un token vencido no se restaura
  it('descarta una sesión guardada cuyo token ya expiró', () => {
    // Deja una sesión vencida guardada
    sessionStorage.setItem(CLAVE, JSON.stringify({ token: crearToken(-60), usuario: USUARIO_PRUEBA }));
    // Servicio, que intenta restaurar la sesión
    const auth = TestBed.inject(AuthService);
    // No hay sesión
    expect(auth.autenticado()).toBe(false);
    // Y se borró lo guardado
    expect(sessionStorage.getItem(CLAVE)).toBeNull();
  });

  // Un texto dañado no rompe la aplicación
  it('ignora una sesión guardada con datos dañados', () => {
    // Deja basura en el almacenamiento
    sessionStorage.setItem(CLAVE, '{no es json');
    // Servicio, que intenta restaurar la sesión
    const auth = TestBed.inject(AuthService);
    // No hay sesión
    expect(auth.autenticado()).toBe(false);
  });

  // La sesión se cierra sola cuando el token vence mientras la página está abierta
  it('cierra la sesión si el token expira mientras se usa', () => {
    // Sesión que vence en 60 segundos
    sessionStorage.setItem(CLAVE, JSON.stringify({ token: crearToken(60), usuario: USUARIO_PRUEBA }));
    // Servicio, que restaura la sesión
    const auth = TestBed.inject(AuthService);
    // Al principio es vigente
    expect(auth.sesionVigente()).toBe(true);
    // Se simula que pasaron dos minutos
    vi.spyOn(Date, 'now').mockReturnValue(Date.now() + 120_000);
    // Ahora ya no es vigente
    expect(auth.sesionVigente()).toBe(false);
    // Y la sesión se cerró
    expect(auth.autenticado()).toBe(false);
    // Restaura el reloj real
    vi.restoreAllMocks();
  });

  // El rol de administrador se detecta
  it('indica si el usuario es administrador', () => {
    // Sesión de un administrador
    sessionStorage.setItem(CLAVE, JSON.stringify({ token: crearToken(3600, 'ADMIN'), usuario: { ...USUARIO_PRUEBA, rol: 'ADMIN' } }));
    // Servicio, que restaura la sesión
    const auth = TestBed.inject(AuthService);
    // Es administrador
    expect(auth.esAdmin()).toBe(true);
  });

  // Después de editar el perfil, la sesión muestra los datos nuevos
  it('actualiza el usuario de la sesión sin cambiar el token', () => {
    // Sesión vigente guardada
    const token = crearToken(3600);
    sessionStorage.setItem(CLAVE, JSON.stringify({ token, usuario: USUARIO_PRUEBA }));
    // Servicio, que restaura la sesión
    const auth = TestBed.inject(AuthService);

    // Cambia el nombre
    auth.actualizarUsuario({ ...USUARIO_PRUEBA, nombre: 'Ana María' });

    // El usuario cambió
    expect(auth.usuario()?.nombre).toBe('Ana María');
    // El token es el mismo
    expect(auth.token()).toBe(token);
    // Y el cambio quedó guardado en el almacenamiento de la pestaña
    expect(sessionStorage.getItem(CLAVE)).toContain('Ana María');
  });
});
