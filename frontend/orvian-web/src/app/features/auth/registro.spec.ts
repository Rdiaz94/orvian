// Utilidades de pruebas de Angular
import { ComponentFixture, TestBed } from '@angular/core/testing';
// Cliente HTTP y su versión de pruebas
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
// Navegación
import { Router, provideRouter } from '@angular/router';

// Utilidades de pruebas
import { USUARIO_PRUEBA, crearToken } from '../../testing/utilidades';
// Pantalla que se prueba
import { RegistroPage } from './registro';

// Pruebas de la pantalla de registro
describe('RegistroPage', () => {
  // Controlador de peticiones simuladas
  let http: HttpTestingController;
  // Componente en prueba
  let fixture: ComponentFixture<RegistroPage>;
  // Raíz del componente
  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;

  // Configura Angular antes de cada prueba
  beforeEach(async () => {
    // Sin sesión guardada
    sessionStorage.clear();
    // Configura el componente, las rutas y el cliente HTTP de pruebas
    await TestBed.configureTestingModule({ imports: [RegistroPage], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    // Obtiene el controlador de peticiones
    http = TestBed.inject(HttpTestingController);
    // Crea el componente
    fixture = TestBed.createComponent(RegistroPage);
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
  it('muestra el logo, los campos y el panel de arte', () => {
    // Aparece el logo
    expect(raiz().querySelector('app-logo svg[aria-label="Logo de Orvian"]')).not.toBeNull();
    // Aparecen los campos principales
    for (const campo of ['#nombre', '#correo', '#contrasena', '#carrera', '#ciclo']) {
      // Cada campo existe
      expect(raiz().querySelector(campo)).not.toBeNull();
    }
    // Aparece el panel de arte
    expect(raiz().querySelector('app-arte-auth')?.textContent).toContain('Tu carrera, más organizada');
  });

  // Validación
  it('no envía el formulario vacío y muestra los avisos', () => {
    // Envía el formulario vacío
    enviar();
    // No se envió ninguna petición
    http.expectNone((peticion) => peticion.url.endsWith('/api/auth/registro'));
    // Se muestran los avisos
    expect(raiz().textContent).toContain('El nombre es obligatorio');
    expect(raiz().textContent).toContain('Debe tener entre 8 y 72 caracteres');
  });

  // Contraseña corta
  it('rechaza una contraseña de menos de 8 caracteres', () => {
    // Escribe datos válidos salvo la contraseña
    escribir('#nombre', 'Ana Pérez');
    escribir('#correo', 'ana@correo.com');
    escribir('#contrasena', '1234567');
    // Envía el formulario
    enviar();
    // No se envió ninguna petición
    http.expectNone((peticion) => peticion.url.endsWith('/api/auth/registro'));
  });

  // Registro correcto
  it('registra, inicia sesión y lleva al inicio', () => {
    // Espía la navegación
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    // Escribe los datos con espacios sobrantes y sin carrera
    escribir('#nombre', '  Ana Pérez  ');
    escribir('#correo', 'ana@correo.com');
    escribir('#contrasena', 'Clave-123456');
    // Envía el formulario
    enviar();
    // La petición de registro lleva los datos limpios
    const registro = http.expectOne((peticion) => peticion.method === 'POST' && peticion.url.endsWith('/api/auth/registro'));
    expect(registro.request.body).toEqual({ nombre: 'Ana Pérez', correo: 'ana@correo.com', contrasena: 'Clave-123456', carrera: null, ciclo: null });
    // La API responde con el usuario creado
    registro.flush(USUARIO_PRUEBA);
    // Después inicia sesión con las mismas credenciales
    const login = http.expectOne((peticion) => peticion.method === 'POST' && peticion.url.endsWith('/api/auth/login'));
    expect(login.request.body).toEqual({ correo: 'ana@correo.com', contrasena: 'Clave-123456' });
    // La API responde con el token
    login.flush({ token: crearToken(3600), tipo: 'Bearer', expiraEnSegundos: 3600, usuario: USUARIO_PRUEBA });
    // Lleva al inicio
    expect(navegar).toHaveBeenCalledWith(['/inicio']);
  });

  // Correo repetido
  it('muestra el aviso del servidor cuando el correo ya existe', () => {
    // Escribe los datos
    escribir('#nombre', 'Ana Pérez');
    escribir('#correo', 'ana@correo.com');
    escribir('#contrasena', 'Clave-123456');
    // Envía el formulario
    enviar();
    // La API responde con un conflicto
    http.expectOne((peticion) => peticion.url.endsWith('/api/auth/registro')).flush({ title: 'Conflicto', detail: 'Ya existe un usuario registrado con el correo ana@correo.com' }, { status: 409, statusText: 'Conflict' });
    // Actualiza la pantalla
    fixture.detectChanges();
    // Se muestra el motivo
    expect(raiz().textContent).toContain('Ya existe un usuario registrado');
  });
});
