// Utilidades de pruebas de Angular
import { ComponentFixture, TestBed } from '@angular/core/testing';
// Cliente HTTP y su versión de pruebas
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

// Tipos de datos
import { Pagina, Usuario } from '../../core/modelos';
// Pantalla que se prueba
import { AdminPage } from './admin';

// Crea un usuario de ejemplo
function crearUsuario(cambios: Partial<Usuario> = {}): Usuario {
  // Usuario base
  return { id: 1, nombre: 'Ana Pérez', correo: 'ana@correo.com', rol: 'ADMIN', carrera: 'Ingeniería de Sistemas', ciclo: 5, ...cambios };
}

// Crea una página de usuarios como la que devuelve la API
function pagina(items: Usuario[], numero = 0, totalPaginas = 1): Pagina<Usuario> {
  // Página con los elementos indicados
  return { content: items, page: { size: 20, number: numero, totalElements: items.length, totalPages: totalPaginas } };
}

// Pruebas de la pantalla de administración
describe('AdminPage', () => {
  // Controlador de peticiones simuladas
  let http: HttpTestingController;
  // Componente en prueba
  let fixture: ComponentFixture<AdminPage>;
  // Raíz del componente
  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;

  // Configura Angular antes de cada prueba
  beforeEach(async () => {
    // Configura el componente y el cliente HTTP de pruebas
    await TestBed.configureTestingModule({ imports: [AdminPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    // Obtiene el controlador de peticiones
    http = TestBed.inject(HttpTestingController);
    // Crea el componente
    fixture = TestBed.createComponent(AdminPage);
  });

  // Comprueba que no quedaron peticiones sin atender
  afterEach(() => http.verify());

  // Abre la pantalla y responde a las consultas iniciales
  function abrir(usuarios: Pagina<Usuario>, cursos: { id: number; nombre: string }[] = []): void {
    // Dispara la carga inicial
    fixture.detectChanges();
    // Responde con los usuarios
    http.expectOne((peticion) => peticion.url.endsWith('/api/admin/usuarios')).flush(usuarios);
    // Responde con los cursos
    http.expectOne((peticion) => peticion.url.endsWith('/api/cursos')).flush(cursos);
    // Actualiza la pantalla
    fixture.detectChanges();
  }

  // Busca un botón por su texto
  function boton(texto: string): HTMLButtonElement {
    // El botón con ese texto
    const encontrado = Array.from(raiz().querySelectorAll('button')).find((candidato) => candidato.textContent?.trim() === texto);
    // Si no existe, la prueba falla con un mensaje claro
    if (!encontrado) {
      // Falla
      throw new Error(`No se encontró el botón "${texto}"`);
    }
    // Devuelve el botón
    return encontrado;
  }

  // Tabla de usuarios
  it('muestra los usuarios con su rol y sin datos sensibles', () => {
    // Abre con dos usuarios
    abrir(pagina([crearUsuario(), crearUsuario({ id: 2, nombre: 'Luis Rojas', correo: 'luis@correo.com', rol: 'ESTUDIANTE', carrera: null, ciclo: null })]));
    // Texto de la pantalla
    const texto = raiz().textContent ?? '';
    // Aparecen los nombres
    expect(texto).toContain('Ana Pérez');
    expect(texto).toContain('Luis Rojas');
    // Aparecen los roles
    expect(texto).toContain('Administrador');
    expect(texto).toContain('Estudiante');
    // Nunca se muestra ninguna contraseña ni hash
    expect(texto.toLowerCase()).not.toContain('hash');
  });

  // Paginación
  it('pagina los usuarios', () => {
    // Abre con una página de dos
    abrir(pagina([crearUsuario()], 0, 2));
    // En la primera página no se puede retroceder
    expect(boton('Anterior').disabled).toBe(true);
    // Pasa a la página siguiente
    boton('Siguiente').click();
    // La consulta pide la página 1
    const siguiente = http.expectOne((peticion) => peticion.url.endsWith('/api/admin/usuarios') && peticion.params.get('pagina') === '1');
    // La API responde con la segunda página
    siguiente.flush(pagina([crearUsuario({ id: 3, nombre: 'María' })], 1, 2));
    // Actualiza la pantalla
    fixture.detectChanges();
    // Se muestra la segunda página
    expect(raiz().textContent).toContain('Página 2 de 2');
    // Ya no se puede avanzar
    expect(boton('Siguiente').disabled).toBe(true);
  });

  // Lista de cursos
  it('muestra los cursos existentes en su pestaña', () => {
    // Abre con dos cursos
    abrir(pagina([]), [{ id: 1, nombre: 'Base de Datos' }, { id: 2, nombre: 'Soluciones Web' }]);
    // Cambia a la pestaña de cursos
    boton('Cursos').click();
    // Actualiza la pantalla
    fixture.detectChanges();
    // Aparecen los cursos
    expect(raiz().textContent).toContain('Base de Datos');
    expect(raiz().textContent).toContain('Soluciones Web');
  });

  // Crear un curso
  it('crea un curso con el nombre limpio y recarga la lista', () => {
    // Abre sin cursos
    abrir(pagina([]));
    // Cambia a la pestaña de cursos
    boton('Cursos').click();
    fixture.detectChanges();
    // Escribe el nombre con espacios sobrantes
    const campo = raiz().querySelector<HTMLInputElement>('input[aria-label="Nombre del nuevo curso"]')!;
    campo.value = '  Calidad de Software  ';
    campo.dispatchEvent(new Event('input'));
    // Envía el formulario
    raiz().querySelector('form')!.dispatchEvent(new Event('submit'));
    // La petición lleva el nombre limpio
    const creacion = http.expectOne((peticion) => peticion.method === 'POST' && peticion.url.endsWith('/api/admin/cursos'));
    expect(creacion.request.body).toEqual({ nombre: 'Calidad de Software' });
    // La API responde con el curso creado
    creacion.flush({ id: 3, nombre: 'Calidad de Software' });
    // Se recarga la lista de cursos
    http.expectOne((peticion) => peticion.method === 'GET' && peticion.url.endsWith('/api/cursos')).flush([{ id: 3, nombre: 'Calidad de Software' }]);
    // Actualiza la pantalla
    fixture.detectChanges();
    // Se avisa del éxito
    expect(raiz().textContent).toContain('Curso "Calidad de Software" creado');
  });

  // Curso repetido
  it('muestra el aviso del servidor si el curso ya existe', () => {
    // Abre sin cursos
    abrir(pagina([]));
    // Cambia a la pestaña de cursos
    boton('Cursos').click();
    fixture.detectChanges();
    // Escribe un nombre
    const campo = raiz().querySelector<HTMLInputElement>('input[aria-label="Nombre del nuevo curso"]')!;
    campo.value = 'Base de Datos';
    campo.dispatchEvent(new Event('input'));
    // Envía el formulario
    raiz().querySelector('form')!.dispatchEvent(new Event('submit'));
    // La API responde con un conflicto
    http.expectOne((peticion) => peticion.method === 'POST').flush({ title: 'Conflicto', detail: 'Ya existe un curso llamado Base de Datos' }, { status: 409, statusText: 'Conflict' });
    // Actualiza la pantalla
    fixture.detectChanges();
    // Se muestra el motivo
    expect(raiz().textContent).toContain('Ya existe un curso llamado Base de Datos');
  });

  // Validación
  it('no envía el formulario si el nombre está vacío', () => {
    // Abre sin cursos
    abrir(pagina([]));
    // Cambia a la pestaña de cursos
    boton('Cursos').click();
    fixture.detectChanges();
    // Envía el formulario vacío
    raiz().querySelector('form')!.dispatchEvent(new Event('submit'));
    // No se envió ninguna creación
    http.expectNone((peticion) => peticion.method === 'POST');
  });
});
