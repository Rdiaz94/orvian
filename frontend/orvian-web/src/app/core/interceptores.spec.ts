// Utilidades de pruebas de Angular
import { TestBed } from '@angular/core/testing';
// Cliente HTTP y su versión de pruebas
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
// Navegación
import { Router, provideRouter } from '@angular/router';

// Dirección de la API
import { environment } from '../../environments/environment';
// Utilidades de pruebas
import { USUARIO_PRUEBA, crearToken } from '../testing/utilidades';
// Servicio de sesión
import { AuthService } from './auth.service';
// Interceptores que se prueban
import { sesionExpiradaInterceptor, tokenInterceptor } from './interceptores';

// Pruebas de los interceptores
describe('interceptores', () => {
  // Cliente HTTP y controlador de peticiones simuladas
  let http: HttpClient;
  let controlador: HttpTestingController;
  // Servicio de sesión
  let auth: AuthService;
  // Token de la sesión de pruebas
  const token = crearToken(3600);

  // Antes de cada prueba se configura Angular con una sesión iniciada
  beforeEach(() => {
    // Sesión vigente guardada, que el servicio restaura
    sessionStorage.setItem('orvian.sesion', JSON.stringify({ token, usuario: USUARIO_PRUEBA }));
    // Configura el cliente HTTP con los interceptores reales
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([tokenInterceptor, sesionExpiradaInterceptor])), provideHttpClientTesting()],
    });
    // Obtiene los componentes de la prueba
    http = TestBed.inject(HttpClient);
    controlador = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  // Limpia lo que dejó cada prueba
  afterEach(() => {
    // Borra la sesión guardada
    sessionStorage.clear();
    // Restaura funciones simuladas
    vi.restoreAllMocks();
  });

  // El token viaja a nuestra API
  it('agrega el token a las peticiones hacia la API', () => {
    // Hace una petición a la API
    http.get(`${environment.apiUrl}/api/tareas`).subscribe();
    // La petición lleva la cabecera de autorización
    expect(controlador.expectOne(`${environment.apiUrl}/api/tareas`).request.headers.get('Authorization')).toBe(`Bearer ${token}`);
  });

  // El token nunca viaja a terceros
  it('no agrega el token a peticiones hacia otros sitios', () => {
    // Hace una petición a otro sitio
    http.get('https://otro-sitio.com/datos').subscribe();
    // La petición no lleva la cabecera de autorización
    expect(controlador.expectOne('https://otro-sitio.com/datos').request.headers.has('Authorization')).toBe(false);
  });

  // Un 401 en una ruta protegida significa sesión vencida
  it('cierra la sesión y lleva al login cuando la API responde 401', () => {
    // Espía la navegación
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    // Hace una petición protegida
    http.get(`${environment.apiUrl}/api/tareas`).subscribe({ error: () => undefined });
    // La API responde 401
    controlador.expectOne(`${environment.apiUrl}/api/tareas`).flush({}, { status: 401, statusText: 'Unauthorized' });
    // La sesión se cerró
    expect(auth.autenticado()).toBe(false);
    // Y se llevó al login indicando el motivo
    expect(navegar).toHaveBeenCalledWith(['/login'], { queryParams: { motivo: 'expirada' } });
  });

  // Un 401 al iniciar sesión es una contraseña incorrecta, no una sesión vencida
  it('no cierra la sesión por un 401 del login', () => {
    // Espía la navegación
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    // Intenta iniciar sesión con credenciales incorrectas
    http.post(`${environment.apiUrl}/api/auth/login`, {}).subscribe({ error: () => undefined });
    // La API responde 401
    controlador.expectOne(`${environment.apiUrl}/api/auth/login`).flush({}, { status: 401, statusText: 'Unauthorized' });
    // No se redirige
    expect(navegar).not.toHaveBeenCalled();
  });
});
