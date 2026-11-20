// Utilidades de pruebas de Angular
import { ComponentFixture, TestBed } from '@angular/core/testing';
// Cliente HTTP y su versión de pruebas
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
// Navegación
import { Router, provideRouter } from '@angular/router';

// Servicio de sesión
import { AuthService } from '../../core/auth.service';
// Tipos de datos
import { ResumenAcademico } from '../../core/modelos';
// Utilidades de pruebas
import { USUARIO_PRUEBA, crearToken } from '../../testing/utilidades';
// Pantalla que se prueba
import { PerfilPage } from './perfil';

// Estadísticas de ejemplo
const RESUMEN: ResumenAcademico = {
  tareasTotales: 8, tareasPendientes: 3, tareasEnProgreso: 1, tareasHechas: 4, porcentajeCumplimiento: 50, gruposActivos: 2, recursosCompartidos: 5, proximaEntrega: null,
};

// Pruebas de la pantalla de perfil
describe('PerfilPage', () => {
  // Controlador de peticiones simuladas
  let http: HttpTestingController;
  // Componente en prueba
  let fixture: ComponentFixture<PerfilPage>;
  // Raíz del componente
  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;

  // Configura Angular antes de cada prueba, con una sesión iniciada
  beforeEach(async () => {
    // Deja una sesión vigente guardada
    sessionStorage.setItem('orvian.sesion', JSON.stringify({ token: crearToken(3600), usuario: USUARIO_PRUEBA }));
    // Configura el componente, las rutas y el cliente HTTP de pruebas
    await TestBed.configureTestingModule({ imports: [PerfilPage], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    // Obtiene el controlador de peticiones
    http = TestBed.inject(HttpTestingController);
    // Crea el componente
    fixture = TestBed.createComponent(PerfilPage);
    // Dispara la carga inicial
    fixture.detectChanges();
    // Responde con las estadísticas
    http.expectOne((peticion) => peticion.url.endsWith('/api/resumen')).flush(RESUMEN);
    // Actualiza la pantalla
    fixture.detectChanges();
  });

  // Comprueba que no quedaron peticiones sin atender y limpia lo simulado
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

  // Envía un formulario
  function enviar(selector: string): void {
    // Dispara el envío del formulario
    raiz().querySelector(selector)!.dispatchEvent(new Event('submit'));
    // Actualiza la pantalla
    fixture.detectChanges();
  }

  // Vista inicial
  it('muestra los datos de la cuenta y las estadísticas', () => {
    // Texto de la pantalla
    const texto = raiz().textContent ?? '';
    // Aparece el nombre
    expect(texto).toContain('Ana Pérez');
    // Aparece el correo
    expect(texto).toContain('ana@correo.com');
    // Aparece el rol
    expect(texto).toContain('Estudiante');
    // Aparecen las estadísticas
    expect(texto).toContain('50%');
    // El formulario trae el nombre actual
    expect(raiz().querySelector<HTMLInputElement>('#nombre')!.value).toBe('Ana Pérez');
  });

  // Edición de datos
  it('guarda los datos personales y actualiza la sesión', () => {
    // Cambia el nombre, deja la carrera en blanco y cambia el ciclo
    escribir('#nombre', '  Ana María  ');
    escribir('#carrera', '');
    escribir('#ciclo', '6');
    // Envía el formulario
    enviar('#form-datos');
    // La petición lleva los datos limpios
    const edicion = http.expectOne((peticion) => peticion.method === 'PUT' && peticion.url.endsWith('/api/perfil'));
    expect(edicion.request.body).toEqual({ nombre: 'Ana María', carrera: null, ciclo: 6 });
    // La API responde con el usuario actualizado
    edicion.flush({ ...USUARIO_PRUEBA, nombre: 'Ana María', carrera: null, ciclo: 6 });
    // Actualiza la pantalla
    fixture.detectChanges();
    // La sesión muestra el nombre nuevo
    expect(TestBed.inject(AuthService).usuario()?.nombre).toBe('Ana María');
    // Se avisa del éxito
    expect(raiz().textContent).toContain('Datos actualizados');
  });

  // Validación de datos
  it('no envía los datos si el nombre está vacío', () => {
    // Borra el nombre
    escribir('#nombre', '');
    // Envía el formulario
    enviar('#form-datos');
    // No se envió nada
    http.expectNone((peticion) => peticion.method === 'PUT');
    // Se muestra el mensaje de validación
    expect(raiz().textContent).toContain('El nombre es obligatorio');
  });

  // Confirmación de la contraseña
  it('no cambia la contraseña si la confirmación no coincide', () => {
    // Escribe contraseñas distintas
    escribir('#actual', 'Actual-123');
    escribir('#nueva', 'Nueva-12345');
    escribir('#confirmacion', 'Otra-12345');
    // Envía el formulario
    enviar('#form-clave');
    // No se envió nada
    http.expectNone((peticion) => peticion.method === 'PUT');
    // Se avisa de que no coinciden
    expect(raiz().textContent).toContain('no coinciden');
  });

  // Cambio de contraseña
  it('cambia la contraseña sin enviar la confirmación y limpia los campos', () => {
    // Escribe las contraseñas
    escribir('#actual', 'Actual-123');
    escribir('#nueva', 'Nueva-12345');
    escribir('#confirmacion', 'Nueva-12345');
    // Envía el formulario
    enviar('#form-clave');
    // La petición lleva solo la actual y la nueva
    const cambio = http.expectOne((peticion) => peticion.method === 'PUT' && peticion.url.endsWith('/api/perfil/contrasena'));
    expect(cambio.request.body).toEqual({ contrasenaActual: 'Actual-123', contrasenaNueva: 'Nueva-12345' });
    // La API responde sin contenido
    cambio.flush(null, { status: 204, statusText: 'No Content' });
    // Actualiza la pantalla
    fixture.detectChanges();
    // Se avisa del éxito
    expect(raiz().textContent).toContain('Contraseña actualizada');
    // Los campos quedan vacíos
    expect(raiz().querySelector<HTMLInputElement>('#nueva')!.value).toBe('');
  });

  // Contraseña actual incorrecta
  it('muestra el error del servidor y no cierra la sesión si la contraseña actual es incorrecta', () => {
    // Escribe las contraseñas
    escribir('#actual', 'Incorrecta-1');
    escribir('#nueva', 'Nueva-12345');
    escribir('#confirmacion', 'Nueva-12345');
    // Envía el formulario
    enviar('#form-clave');
    // La API rechaza el cambio
    http.expectOne((peticion) => peticion.url.endsWith('/api/perfil/contrasena')).flush({ title: 'Solicitud inválida', detail: 'La contraseña actual no es correcta' }, { status: 400, statusText: 'Bad Request' });
    // Actualiza la pantalla
    fixture.detectChanges();
    // Se muestra el motivo
    expect(raiz().textContent).toContain('La contraseña actual no es correcta');
    // La sesión sigue abierta
    expect(TestBed.inject(AuthService).autenticado()).toBe(true);
  });

  // Cierre de sesión
  it('cierra la sesión y vuelve al login', () => {
    // Espía la navegación
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    // Pulsa Cerrar sesión
    Array.from(raiz().querySelectorAll('button')).find((boton) => boton.textContent?.includes('Cerrar sesión'))!.click();
    // Ya no hay sesión
    expect(TestBed.inject(AuthService).autenticado()).toBe(false);
    // Vuelve al login
    expect(navegar).toHaveBeenCalledWith(['/login']);
  });
});
