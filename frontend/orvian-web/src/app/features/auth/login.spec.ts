// Utilidades de pruebas de Angular
import { ComponentFixture, TestBed } from '@angular/core/testing';
// Cliente HTTP y su versión de pruebas
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
// Navegación
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

// Utilidades de pruebas
import { USUARIO_PRUEBA, crearToken } from '../../testing/utilidades';
// Pantalla que se prueba
import { LoginPage } from './login';

// Pruebas de la pantalla de inicio de sesión
describe('LoginPage', () => {
  // Controlador de peticiones simuladas
  let http: HttpTestingController;
  // Componente en prueba
  let fixture: ComponentFixture<LoginPage>;
  // Raíz del componente
  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;

  // Configura Angular antes de cada prueba
  beforeEach(async () => {
    // Sin sesión guardada
    sessionStorage.clear();
    // Configura el componente, las rutas y el cliente HTTP de pruebas
    await TestBed.configureTestingModule({ imports: [LoginPage], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    // Obtiene el controlador de peticiones
    http = TestBed.inject(HttpTestingController);
    // Crea el componente
    fixture = TestBed.createComponent(LoginPage);
    // Dibuja la pantalla
    fixture.detectChanges();
  });

  // Comprueba que no quedaron peticiones sin atender y restaura lo simulado
  afterEach(() => {
    // Verifica las peticiones pendientes
    http.verify();
    // Restaura las funciones simuladas
    vi.restoreAllMocks();
    // Borra la sesión de la prueba
    sessionStorage.clear();
  });

  // Escribe un valor en un campo como lo haría una persona
  function escribir(selector: string, valor: string): void {
    // Campo buscado
    const campo = raiz().querySelector<HTMLInputElement>(selector)!;
    // Asigna el valor
    campo.value = valor;
    // Avisa a Angular del cambio
    campo.dispatchEvent(new Event('input'));
  }

  // Envía el formulario
  function enviar(): void {
    // Dispara el envío
    raiz().querySelector('form')!.dispatchEvent(new Event('submit'));
    // Actualiza la pantalla
    fixture.detectChanges();
  }

  // Marca de la aplicación
  it('muestra el logo, el formulario y el panel de arte', () => {
    // Aparece el logo
    expect(raiz().querySelector('app-logo svg[aria-label="Logo de Orvian"]')).not.toBeNull();
    // Aparece el formulario con sus dos campos
    expect(raiz().querySelector('#correo')).not.toBeNull();
    expect(raiz().querySelector('#contrasena')).not.toBeNull();
    // Aparece el panel de arte con su mensaje
    expect(raiz().querySelector('app-arte-auth')?.textContent).toContain('Bienvenido a Orvian');
  });

  // Validación
  it('no envía el formulario vacío y muestra los avisos', () => {
    // Envía el formulario vacío
    enviar();
    // No se envió ninguna petición
    http.expectNone((peticion) => peticion.url.endsWith('/api/auth/login'));
    // Se muestran los avisos de cada campo
    expect(raiz().textContent).toContain('Escribe un correo válido');
    expect(raiz().textContent).toContain('Escribe tu contraseña');
  });

  // Inicio de sesión correcto
  it('inicia sesión y lleva al inicio', () => {
    // Espía la navegación
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    // Escribe las credenciales
    escribir('#correo', 'ana@correo.com');
    escribir('#contrasena', 'Clave-123456');
    // Envía el formulario
    enviar();
    // La petición lleva las credenciales
    const login = http.expectOne((peticion) => peticion.method === 'POST' && peticion.url.endsWith('/api/auth/login'));
    expect(login.request.body).toEqual({ correo: 'ana@correo.com', contrasena: 'Clave-123456' });
    // La API responde con el token
    login.flush({ token: crearToken(3600), tipo: 'Bearer', expiraEnSegundos: 3600, usuario: USUARIO_PRUEBA });
    // Lleva al inicio
    expect(navegar).toHaveBeenCalledWith(['/inicio']);
  });

  // Credenciales incorrectas
  it('muestra el error de la API y vuelve a habilitar el botón', () => {
    // Escribe las credenciales
    escribir('#correo', 'ana@correo.com');
    escribir('#contrasena', 'incorrecta');
    // Envía el formulario
    enviar();
    // La API rechaza las credenciales
    http.expectOne((peticion) => peticion.url.endsWith('/api/auth/login')).flush({ title: 'Credenciales inválidas', detail: 'Correo o contraseña incorrectos' }, { status: 401, statusText: 'Unauthorized' });
    // Actualiza la pantalla
    fixture.detectChanges();
    // Se muestra el motivo
    expect(raiz().textContent).toContain('Correo o contraseña incorrectos');
    // El botón de envío vuelve a estar habilitado
    expect(raiz().querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(false);
  });

  // Ver contraseña
  it('alterna la visibilidad de la contraseña', () => {
    // Campo de la contraseña
    const campo = raiz().querySelector<HTMLInputElement>('#contrasena')!;
    // Al inicio está oculta
    expect(campo.type).toBe('password');
    // Pulsa el botón del ojo
    raiz().querySelector<HTMLButtonElement>('button[aria-label="Mostrar contraseña"]')!.click();
    // Actualiza la pantalla
    fixture.detectChanges();
    // Ahora se ve en texto
    expect(campo.type).toBe('text');
  });
});

// Pruebas del aviso de sesión vencida, que depende de la dirección
describe('LoginPage con sesión vencida', () => {
  // Muestra el aviso cuando la dirección lo indica
  it('muestra el aviso cuando llega con motivo=expirada', async () => {
    // Configura Angular con la ruta de login
    TestBed.configureTestingModule({ providers: [provideRouter([{ path: 'login', component: LoginPage }]), provideHttpClient(), provideHttpClientTesting()] });
    // Navega a la dirección con el motivo
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/login?motivo=expirada', LoginPage);
    // Actualiza la pantalla
    harness.detectChanges();
    // Se muestra el aviso
    expect((harness.routeNativeElement as HTMLElement).textContent).toContain('Tu sesión venció');
  });
});
