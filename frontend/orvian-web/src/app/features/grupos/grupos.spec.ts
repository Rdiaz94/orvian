// Utilidades de pruebas de Angular
import { ComponentFixture, TestBed } from '@angular/core/testing';
// Cliente HTTP y su versión de pruebas
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
// Navegación
import { Router, provideRouter } from '@angular/router';

// Tipos de datos
import { Grupo, Pagina } from '../../core/modelos';
// Pantalla que se prueba
import { GruposPage } from './grupos';

// Crea un grupo de ejemplo
function crearGrupo(cambios: Partial<Grupo> = {}): Grupo {
  // Grupo base
  return { id: 7, nombre: 'Orvian Team', descripcion: 'Proyecto final', cursoId: null, cursoNombre: 'Soluciones Web', miRol: 'LIDER', ...cambios };
}

// Crea una página de resultados como la que devuelve la API
function pagina(items: Grupo[]): Pagina<Grupo> {
  // Página con los elementos indicados
  return { content: items, page: { size: 12, number: 0, totalElements: items.length, totalPages: 1 } };
}

// Pruebas de la lista de grupos
describe('GruposPage', () => {
  // Controlador de peticiones simuladas
  let http: HttpTestingController;
  // Componente en prueba
  let fixture: ComponentFixture<GruposPage>;
  // Raíz del componente
  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;

  // Configura Angular antes de cada prueba
  beforeEach(async () => {
    // Configura el componente, las rutas y el cliente HTTP de pruebas
    await TestBed.configureTestingModule({ imports: [GruposPage], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    // Obtiene el controlador de peticiones
    http = TestBed.inject(HttpTestingController);
    // Crea el componente
    fixture = TestBed.createComponent(GruposPage);
  });

  // Comprueba que no quedaron peticiones sin atender y restaura las funciones simuladas
  afterEach(() => {
    // Verifica las peticiones pendientes
    http.verify();
    // Restaura las funciones simuladas
    vi.restoreAllMocks();
  });

  // Abre la pantalla y responde a las consultas iniciales
  function abrir(grupos: Grupo[]): void {
    // Dispara la carga inicial
    fixture.detectChanges();
    // Responde con los grupos
    http.expectOne((peticion) => peticion.url.endsWith('/api/grupos')).flush(pagina(grupos));
    // Responde con los cursos
    http.expectOne((peticion) => peticion.url.endsWith('/api/cursos')).flush([{ id: 1, nombre: 'Soluciones Web' }]);
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

  // Lista con datos
  it('muestra los grupos con su papel y un enlace al detalle', () => {
    // Abre con un grupo
    abrir([crearGrupo()]);
    // Texto de la pantalla
    expect(raiz().textContent).toContain('Orvian Team');
    // Muestra el papel
    expect(raiz().textContent).toContain('Líder');
    // El enlace lleva al detalle del grupo
    expect(raiz().querySelector('a[href="/grupos/7"]')).not.toBeNull();
  });

  // Lista vacía
  it('invita a crear un grupo cuando no hay ninguno', () => {
    // Abre sin grupos
    abrir([]);
    // Muestra el mensaje
    expect(raiz().textContent).toContain('Todavía no perteneces a ningún grupo');
  });

  // Crear un grupo
  it('crea un grupo y lleva a su detalle', () => {
    // Espía la navegación
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    // Abre sin grupos
    abrir([]);
    // Abre el diálogo
    boton('Crear grupo').click();
    // Actualiza la pantalla
    fixture.detectChanges();
    // Escribe el nombre
    const nombre = raiz().querySelector<HTMLInputElement>('#nombre')!;
    nombre.value = '  Equipo Alfa  ';
    nombre.dispatchEvent(new Event('input'));
    // Envía el formulario
    raiz().querySelector('app-grupo-formulario form')!.dispatchEvent(new Event('submit'));
    // La petición lleva los datos esperados
    const creacion = http.expectOne((peticion) => peticion.method === 'POST' && peticion.url.endsWith('/api/grupos'));
    // Nombre sin espacios sobrantes
    expect(creacion.request.body).toEqual({ nombre: 'Equipo Alfa', descripcion: null, cursoId: null });
    // La API responde con el grupo creado
    creacion.flush(crearGrupo({ id: 9, nombre: 'Equipo Alfa' }));
    // Lleva al detalle del grupo nuevo
    expect(navegar).toHaveBeenCalledWith(['/grupos', 9]);
  });

  // Validación
  it('no envía el formulario si falta el nombre', () => {
    // Abre sin grupos
    abrir([]);
    // Abre el diálogo
    boton('Crear grupo').click();
    // Actualiza la pantalla
    fixture.detectChanges();
    // Envía el formulario vacío
    raiz().querySelector('app-grupo-formulario form')!.dispatchEvent(new Event('submit'));
    // Actualiza la pantalla
    fixture.detectChanges();
    // No se envió ninguna creación
    http.expectNone((peticion) => peticion.method === 'POST');
    // Se muestra el mensaje de validación
    expect(raiz().textContent).toContain('El nombre es obligatorio');
  });
});
