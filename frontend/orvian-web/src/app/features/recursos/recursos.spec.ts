// Utilidades de pruebas de Angular
import { ComponentFixture, TestBed } from '@angular/core/testing';
// Cliente HTTP y su versión de pruebas
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
// Navegación
import { Router, provideRouter } from '@angular/router';

// Tipos de datos
import { Pagina, Recurso } from '../../core/modelos';
// Pantalla que se prueba
import { RecursosPage } from './recursos';

// Crea un recurso de ejemplo; los datos que se pasan reemplazan a los de ejemplo
function crearRecurso(cambios: Partial<Recurso> = {}): Recurso {
  // Recurso base
  return {
    id: 1, titulo: 'Guía de Spring', tipo: 'PDF', url: 'https://ejemplo.com/guia.pdf', descripcion: 'Material de apoyo', cursoId: null, cursoNombre: null,
    grupoId: null, grupoNombre: null, autorId: 2, autorNombre: 'Luis', descargas: 0, fechaCreacion: '2026-11-01T10:00:00Z', esAutor: false, ...cambios,
  };
}

// Crea una página de resultados como la que devuelve la API
function pagina(items: Recurso[]): Pagina<Recurso> {
  // Página con los elementos indicados
  return { content: items, page: { size: 12, number: 0, totalElements: items.length, totalPages: 1 } };
}

// Pruebas de la biblioteca de recursos
describe('RecursosPage', () => {
  // Controlador de peticiones simuladas
  let http: HttpTestingController;
  // Componente en prueba
  let fixture: ComponentFixture<RecursosPage>;
  // Raíz del componente
  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;

  // Configura Angular antes de cada prueba
  beforeEach(async () => {
    // Configura el componente, las rutas y el cliente HTTP de pruebas
    await TestBed.configureTestingModule({ imports: [RecursosPage], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    // Obtiene el controlador de peticiones
    http = TestBed.inject(HttpTestingController);
    // Crea el componente
    fixture = TestBed.createComponent(RecursosPage);
  });

  // Comprueba que no quedaron peticiones sin atender y restaura las funciones simuladas
  afterEach(() => {
    // Verifica las peticiones pendientes
    http.verify();
    // Restaura las funciones simuladas
    vi.restoreAllMocks();
  });

  // Busca una consulta de recursos por su orden
  function consulta(orden: 'POPULARES' | 'RECIENTES'): TestRequest {
    // La consulta de la lista con ese orden
    return http.expectOne((peticion) => peticion.method === 'GET' && peticion.url.endsWith('/api/recursos') && peticion.params.get('orden') === orden);
  }

  // Abre la pantalla y responde a las consultas iniciales
  function abrir(recientes: Recurso[] = [], populares: Recurso[] = []): void {
    // Dispara la carga inicial
    fixture.detectChanges();
    // Responde con la lista principal
    consulta('RECIENTES').flush(pagina(recientes));
    // Responde con el Top 10
    consulta('POPULARES').flush(pagina(populares));
    // Actualiza la pantalla
    fixture.detectChanges();
  }

  // Busca un botón por su texto
  function boton(texto: string): HTMLButtonElement {
    // Todos los botones de la pantalla
    const encontrado = Array.from(raiz().querySelectorAll('button')).find((candidato) => candidato.textContent?.trim() === texto);
    // Si no existe, la prueba falla con un mensaje claro
    if (!encontrado) {
      // Falla
      throw new Error(`No se encontró el botón "${texto}"`);
    }
    // Devuelve el botón
    return encontrado;
  }

  // Muestra la biblioteca y el Top 10
  it('muestra la biblioteca y el Top 10 con su ranking', () => {
    // Abre con un recurso reciente y uno popular
    abrir([crearRecurso()], [crearRecurso({ id: 2, titulo: 'Tutorial de Angular', descargas: 9 })]);
    // Texto de la pantalla
    const texto = raiz().textContent ?? '';
    // Aparece el recurso de la biblioteca
    expect(texto).toContain('Guía de Spring');
    // Aparece el recurso del Top 10
    expect(texto).toContain('Tutorial de Angular');
    // Aparece el encabezado del Top 10
    expect(texto).toContain('Top 10');
  });

  // Filtro por tipo
  it('filtra por tipo y consulta desde la primera página', () => {
    // Abre la pantalla vacía
    abrir();
    // Pulsa el filtro de video
    boton('VIDEO').click();
    // La consulta lleva el tipo elegido
    const filtrada = http.expectOne((peticion) => peticion.url.endsWith('/api/recursos') && peticion.params.get('tipo') === 'VIDEO');
    // Y empieza en la primera página
    expect(filtrada.request.params.get('pagina')).toBe('0');
    // La API responde sin resultados
    filtrada.flush(pagina([]));
    // Actualiza la pantalla
    fixture.detectChanges();
    // Se muestra el mensaje de búsqueda sin resultados
    expect(raiz().textContent).toContain('No hay recursos que coincidan');
  });

  // Orden por populares
  it('cambia el orden a populares', () => {
    // Abre la pantalla vacía
    abrir();
    // Pulsa el orden por populares
    boton('Populares').click();
    // La lista se consulta con ese orden
    consulta('POPULARES').flush(pagina([]));
  });

  // Búsqueda recibida por la dirección
  it('busca por el texto de la dirección y oculta el Top 10', () => {
    // El texto llega como parámetro de la dirección
    fixture.componentRef.setInput('q', 'spring');
    // Dispara la carga inicial
    fixture.detectChanges();
    // La lista se consulta con el texto
    http.expectOne((peticion) => peticion.url.endsWith('/api/recursos') && peticion.params.get('q') === 'spring' && peticion.params.get('orden') === 'RECIENTES').flush(pagina([crearRecurso()]));
    // El Top 10 también se consulta
    consulta('POPULARES').flush(pagina([crearRecurso({ id: 2, titulo: 'Solo en el Top' })]));
    // Actualiza la pantalla
    fixture.detectChanges();
    // Con búsqueda activa no se muestra el Top 10
    expect(raiz().textContent).not.toContain('Top 10');
    // Se muestra el resultado
    expect(raiz().textContent).toContain('Resultados (1)');
  });

  // El buscador de la pantalla navega
  it('el buscador lleva el texto a la dirección', () => {
    // Espía la navegación
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    // Abre la pantalla vacía
    abrir();
    // Escribe en el buscador
    const campo = raiz().querySelector<HTMLInputElement>('input[type="search"]')!;
    campo.value = ' angular ';
    campo.dispatchEvent(new Event('input'));
    // Envía el formulario de búsqueda
    raiz().querySelector('form[role="search"]')!.dispatchEvent(new Event('submit'));
    // Navega con el texto sin espacios sobrantes
    expect(navegar).toHaveBeenCalledWith(['/recursos'], { queryParams: { q: 'angular' } });
  });

  // Abrir un recurso
  it('al abrir registra la descarga y abre el enlace en otra pestaña', () => {
    // Espía la apertura de ventanas
    const abrirVentana = vi.spyOn(window, 'open').mockReturnValue(null);
    // Abre con un recurso
    abrir([crearRecurso()]);
    // Pulsa Abrir
    boton('Abrir').click();
    // La petición registra la descarga
    const descarga = http.expectOne((peticion) => peticion.method === 'POST' && peticion.url.endsWith('/api/recursos/1/descarga'));
    // La API devuelve el enlace
    descarga.flush({ url: 'https://ejemplo.com/guia.pdf' });
    // Actualiza la pantalla
    fixture.detectChanges();
    // El enlace se abre en otra pestaña sin dar acceso a la actual
    expect(abrirVentana).toHaveBeenCalledWith('https://ejemplo.com/guia.pdf', '_blank', 'noopener,noreferrer');
    // El contador subió en pantalla
    expect(raiz().textContent).toContain('1 descargas');
  });

  // Enlaces peligrosos
  it('no abre un enlace que no sea http o https', () => {
    // Espía la apertura de ventanas
    const abrirVentana = vi.spyOn(window, 'open').mockReturnValue(null);
    // Abre con un recurso
    abrir([crearRecurso()]);
    // Pulsa Abrir
    boton('Abrir').click();
    // La API devuelve un enlace peligroso
    http.expectOne((peticion) => peticion.url.endsWith('/api/recursos/1/descarga')).flush({ url: 'javascript:alert(1)' });
    // Actualiza la pantalla
    fixture.detectChanges();
    // No se abrió nada
    expect(abrirVentana).not.toHaveBeenCalled();
    // Se avisa de que el enlace no es seguro
    expect(raiz().textContent).toContain('no es seguro');
  });

  // Permisos en pantalla
  it('muestra editar y borrar solo en los recursos propios', () => {
    // Abre con un recurso ajeno y uno propio
    abrir([crearRecurso(), crearRecurso({ id: 3, titulo: 'Mi apunte', esAutor: true })]);
    // Etiquetas de accesibilidad de todos los botones
    const etiquetas = Array.from(raiz().querySelectorAll('button')).map((candidato) => candidato.getAttribute('aria-label') ?? '');
    // El recurso propio tiene editar
    expect(etiquetas).toContain('Editar Mi apunte');
    // Y borrar
    expect(etiquetas).toContain('Borrar Mi apunte');
    // El ajeno no tiene editar
    expect(etiquetas).not.toContain('Editar Guía de Spring');
  });

  // Borrado con confirmación
  it('pide confirmación antes de borrar y recarga las listas', () => {
    // Abre con un recurso propio
    abrir([crearRecurso({ id: 3, titulo: 'Mi apunte', esAutor: true })]);
    // Pulsa borrar
    raiz().querySelector<HTMLButtonElement>('button[aria-label="Borrar Mi apunte"]')!.click();
    // Actualiza la pantalla
    fixture.detectChanges();
    // Todavía no se envió nada
    http.expectNone((peticion) => peticion.method === 'DELETE');
    // Confirma
    boton('Sí, borrar').click();
    // Se envía la orden de borrado
    http.expectOne((peticion) => peticion.method === 'DELETE' && peticion.url.endsWith('/api/recursos/3')).flush(null, { status: 204, statusText: 'No Content' });
    // Se recargan las dos listas
    consulta('RECIENTES').flush(pagina([]));
    consulta('POPULARES').flush(pagina([]));
  });

  // Compartir un recurso
  it('comparte un recurso enviando los datos limpios y recarga', () => {
    // Abre la pantalla vacía
    abrir();
    // Abre el diálogo
    boton('Compartir recurso').click();
    // Se piden los cursos y los grupos para las listas del formulario
    http.expectOne((peticion) => peticion.url.endsWith('/api/cursos')).flush([]);
    http.expectOne((peticion) => peticion.url.endsWith('/api/grupos')).flush(pagina([]));
    // Actualiza la pantalla
    fixture.detectChanges();
    // Escribe el título y el enlace
    const titulo = raiz().querySelector<HTMLInputElement>('#titulo')!;
    titulo.value = '  Tutorial  ';
    titulo.dispatchEvent(new Event('input'));
    const enlace = raiz().querySelector<HTMLInputElement>('#url')!;
    enlace.value = 'https://ejemplo.com/tutorial';
    enlace.dispatchEvent(new Event('input'));
    // Envía el formulario
    raiz().querySelector('app-recurso-formulario form')!.dispatchEvent(new Event('submit'));
    // La petición lleva los datos esperados
    const creacion = http.expectOne((peticion) => peticion.method === 'POST' && peticion.url.endsWith('/api/recursos'));
    // Título sin espacios sobrantes y recurso público
    expect(creacion.request.body).toEqual({ titulo: 'Tutorial', tipo: 'PDF', url: 'https://ejemplo.com/tutorial', descripcion: null, cursoId: null, grupoId: null });
    // La API responde con el recurso creado
    creacion.flush(crearRecurso({ titulo: 'Tutorial' }));
    // Se recargan las dos listas
    consulta('RECIENTES').flush(pagina([crearRecurso({ titulo: 'Tutorial' })]));
    consulta('POPULARES').flush(pagina([]));
    // Actualiza la pantalla
    fixture.detectChanges();
    // El diálogo se cerró
    expect(raiz().querySelector('app-recurso-formulario')).toBeNull();
  });

  // Validación del enlace
  it('no envía el formulario si el enlace no es http o https', () => {
    // Abre la pantalla vacía
    abrir();
    // Abre el diálogo
    boton('Compartir recurso').click();
    // Se piden los cursos y los grupos
    http.expectOne((peticion) => peticion.url.endsWith('/api/cursos')).flush([]);
    http.expectOne((peticion) => peticion.url.endsWith('/api/grupos')).flush(pagina([]));
    // Actualiza la pantalla
    fixture.detectChanges();
    // Escribe un título y un enlace peligroso
    const titulo = raiz().querySelector<HTMLInputElement>('#titulo')!;
    titulo.value = 'Trampa';
    titulo.dispatchEvent(new Event('input'));
    const enlace = raiz().querySelector<HTMLInputElement>('#url')!;
    enlace.value = 'javascript:alert(1)';
    enlace.dispatchEvent(new Event('input'));
    // Envía el formulario
    raiz().querySelector('app-recurso-formulario form')!.dispatchEvent(new Event('submit'));
    // Actualiza la pantalla
    fixture.detectChanges();
    // No se envió ninguna creación
    http.expectNone((peticion) => peticion.method === 'POST');
    // Se muestra el mensaje de validación
    expect(raiz().textContent).toContain('Debe empezar con http');
  });
});
