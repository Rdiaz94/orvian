// Utilidades de pruebas de Angular
import { ComponentFixture, TestBed } from '@angular/core/testing';
// Cliente HTTP y su versión de pruebas
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';

// Tipos de datos
import { EstadoTarea, Pagina, Tarea } from '../../core/modelos';
// Pantalla que se prueba
import { TareasPage } from './tareas';

// Crea una tarea de ejemplo; los datos que se pasan reemplazan a los de ejemplo
function crearTarea(cambios: Partial<Tarea> = {}): Tarea {
  // Tarea base
  return {
    id: 1, titulo: 'Capítulo 1', descripcion: null, cursoId: null, cursoNombre: 'Soluciones Web', grupoId: null, grupoNombre: null,
    responsableId: 1, responsableNombre: 'Ana', estado: 'PENDIENTE', etiqueta: 'PROYECTO', fechaVencimiento: null, progreso: 0,
    fechaCreacion: '2026-11-01T10:00:00Z', ...cambios,
  };
}

// Crea una página de resultados como la que devuelve la API
function pagina(items: Tarea[]): Pagina<Tarea> {
  // Página con los elementos indicados
  return { content: items, page: { size: 20, number: 0, totalElements: items.length, totalPages: 1 } };
}

// Pruebas del tablero de tareas
describe('TareasPage', () => {
  // Controlador de peticiones simuladas
  let http: HttpTestingController;
  // Componente en prueba
  let fixture: ComponentFixture<TareasPage>;

  // Configura Angular antes de cada prueba
  beforeEach(async () => {
    // Configura el componente y el cliente HTTP de pruebas
    await TestBed.configureTestingModule({ imports: [TareasPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    // Obtiene el controlador de peticiones
    http = TestBed.inject(HttpTestingController);
    // Crea el componente
    fixture = TestBed.createComponent(TareasPage);
  });

  // Comprueba que no quedaron peticiones sin atender
  afterEach(() => http.verify());

  // Busca la petición de tareas de un estado
  function peticionDeTareas(estado: EstadoTarea): TestRequest {
    // La consulta de tareas de ese estado
    return http.expectOne((peticion) => peticion.method === 'GET' && peticion.url.endsWith('/api/tareas') && peticion.params.get('estado') === estado);
  }

  // Abre la pantalla y responde a las cuatro consultas iniciales
  function abrir(pendientes: Tarea[] = [], enProgreso: Tarea[] = [], hechas: Tarea[] = []): void {
    // Dispara la carga inicial
    fixture.detectChanges();
    // Responde con los cursos
    http.expectOne((peticion) => peticion.url.endsWith('/api/cursos')).flush([{ id: 1, nombre: 'Soluciones Web' }]);
    // Responde con cada columna
    peticionDeTareas('PENDIENTE').flush(pagina(pendientes));
    peticionDeTareas('EN_PROGRESO').flush(pagina(enProgreso));
    peticionDeTareas('HECHA').flush(pagina(hechas));
    // Actualiza la pantalla
    fixture.detectChanges();
  }

  // Busca un botón por su texto
  function boton(texto: string): HTMLButtonElement {
    // Todos los botones de la pantalla
    const botones = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'));
    // El que tiene el texto buscado
    const encontrado = botones.find((candidato) => candidato.textContent?.trim() === texto);
    // Si no existe, la prueba falla con un mensaje claro
    if (!encontrado) {
      // Falla
      throw new Error(`No se encontró el botón "${texto}"`);
    }
    // Devuelve el botón
    return encontrado;
  }

  // Las tareas aparecen en su columna
  it('muestra cada tarea en su columna con los totales', () => {
    // Abre con una tarea pendiente y una hecha
    abrir([crearTarea()], [], [crearTarea({ id: 2, titulo: 'Entregada', estado: 'HECHA', progreso: 100 })]);
    // Texto de la pantalla
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    // Aparecen los títulos
    expect(texto).toContain('Capítulo 1');
    // Aparece la tarea hecha
    expect(texto).toContain('Entregada');
    // Las columnas vacías lo indican
    expect(texto).toContain('Sin tareas');
  });

  // Empezar una tarea
  it('al empezar una tarea envía el avance inicial y recarga las dos columnas', () => {
    // Abre con una tarea pendiente
    abrir([crearTarea()]);
    // Pulsa Empezar
    boton('Empezar').click();
    // La petición cambia el estado con el avance inicial
    const cambio = http.expectOne((peticion) => peticion.method === 'PATCH' && peticion.url.endsWith('/api/tareas/1/avance'));
    // Con el cuerpo esperado
    expect(cambio.request.body).toEqual({ estado: 'EN_PROGRESO', progreso: 10 });
    // La API responde con la tarea actualizada
    cambio.flush(crearTarea({ estado: 'EN_PROGRESO', progreso: 10 }));
    // Se recargan las columnas de origen y destino
    peticionDeTareas('PENDIENTE').flush(pagina([]));
    peticionDeTareas('EN_PROGRESO').flush(pagina([crearTarea({ estado: 'EN_PROGRESO', progreso: 10 })]));
    // Actualiza la pantalla
    fixture.detectChanges();
    // La tarea quedó en progreso y se muestra su avance
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('10%');
  });

  // Las tareas de grupo las administra el líder
  it('no ofrece editar ni borrar las tareas de grupo', () => {
    // Abre con una tarea de grupo
    abrir([crearTarea({ grupoId: 3, grupoNombre: 'Orvian Team' })]);
    // Etiquetas de accesibilidad de todos los botones
    const etiquetas = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).map((candidato) => candidato.getAttribute('aria-label') ?? '');
    // No hay botón de editar
    expect(etiquetas.some((etiqueta) => etiqueta.startsWith('Editar'))).toBe(false);
    // No hay botón de borrar
    expect(etiquetas.some((etiqueta) => etiqueta.startsWith('Borrar'))).toBe(false);
    // Se muestra el nombre del grupo
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Orvian Team');
  });

  // Borrar con confirmación
  it('pide confirmación antes de borrar y recarga la columna', () => {
    // Abre con una tarea pendiente
    abrir([crearTarea()]);
    // Pulsa el botón de borrar
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button[aria-label="Borrar Capítulo 1"]')?.click();
    // Actualiza la pantalla
    fixture.detectChanges();
    // Todavía no se envió nada: antes se pide confirmar
    http.expectNone((peticion) => peticion.method === 'DELETE');
    // Confirma el borrado
    boton('Sí, borrar').click();
    // Se envía la orden de borrado
    http.expectOne((peticion) => peticion.method === 'DELETE' && peticion.url.endsWith('/api/tareas/1')).flush(null, { status: 204, statusText: 'No Content' });
    // Se recarga la columna
    peticionDeTareas('PENDIENTE').flush(pagina([]));
  });

  // Crear una tarea
  it('crea una tarea enviando la fecha como instante UTC', () => {
    // Abre el tablero vacío
    abrir();
    // Abre el diálogo
    boton('Nueva tarea').click();
    // Actualiza la pantalla
    fixture.detectChanges();
    // Raíz de la pantalla
    const raiz = fixture.nativeElement as HTMLElement;
    // Escribe el título
    const titulo = raiz.querySelector<HTMLInputElement>('#titulo');
    titulo!.value = '  Informe final  ';
    titulo!.dispatchEvent(new Event('input'));
    // Escribe la fecha límite
    const fecha = raiz.querySelector<HTMLInputElement>('#fecha');
    fecha!.value = '2026-11-05T14:30';
    fecha!.dispatchEvent(new Event('input'));
    // Envía el formulario
    raiz.querySelector('form')!.dispatchEvent(new Event('submit'));
    // La petición lleva los datos esperados
    const creacion = http.expectOne((peticion) => peticion.method === 'POST' && peticion.url.endsWith('/api/tareas'));
    // El título viaja sin espacios sobrantes y la fecha como instante UTC
    expect(creacion.request.body).toEqual({ titulo: 'Informe final', descripcion: null, cursoId: null, etiqueta: 'OTRO', fechaVencimiento: new Date('2026-11-05T14:30').toISOString() });
    // La API responde con la tarea creada
    creacion.flush(crearTarea({ titulo: 'Informe final' }));
    // Se recarga todo el tablero
    peticionDeTareas('PENDIENTE').flush(pagina([crearTarea({ titulo: 'Informe final' })]));
    peticionDeTareas('EN_PROGRESO').flush(pagina([]));
    peticionDeTareas('HECHA').flush(pagina([]));
    // Actualiza la pantalla
    fixture.detectChanges();
    // El diálogo se cerró
    expect(raiz.querySelector('app-tarea-formulario')).toBeNull();
  });

  // Validación del formulario
  it('no envía el formulario si falta el título', () => {
    // Abre el tablero vacío
    abrir();
    // Abre el diálogo
    boton('Nueva tarea').click();
    // Actualiza la pantalla
    fixture.detectChanges();
    // Envía el formulario vacío
    (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
    // Actualiza la pantalla
    fixture.detectChanges();
    // No se envió ninguna creación
    http.expectNone((peticion) => peticion.method === 'POST');
    // Se muestra el mensaje de validación
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('El título es obligatorio');
  });
});
