// Utilidades de pruebas de Angular
import { ComponentFixture, TestBed } from '@angular/core/testing';
// Cliente HTTP y su versión de pruebas
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
// Navegación
import { Router, provideRouter } from '@angular/router';

// Utilidades de pruebas
import { USUARIO_PRUEBA, crearToken } from '../../testing/utilidades';
// Tipos de datos
import { GrupoDetalle, Pagina, RolGrupo, Tarea } from '../../core/modelos';
// Pantalla que se prueba
import { GrupoDetallePage } from './grupo-detalle';

// Crea un grupo con dos integrantes: el usuario de la sesión (id 1) y Luis (id 2)
function crearDetalle(miRol: RolGrupo): GrupoDetalle {
  // El otro integrante tiene el papel contrario
  const rolDeLuis: RolGrupo = miRol === 'LIDER' ? 'MIEMBRO' : 'LIDER';
  // Detalle completo
  return {
    grupo: { id: 7, nombre: 'Orvian Team', descripcion: null, cursoId: null, cursoNombre: null, miRol },
    integrantes: [
      { usuarioId: 1, nombre: 'Ana Pérez', carrera: null, ciclo: 5, rolGrupo: miRol, fechaIngreso: '2026-11-01T10:00:00Z' },
      { usuarioId: 2, nombre: 'Luis Rojas', carrera: null, ciclo: 4, rolGrupo: rolDeLuis, fechaIngreso: '2026-11-01T10:00:00Z' },
    ],
  };
}

// Crea una tarea de grupo de ejemplo
function crearTarea(cambios: Partial<Tarea> = {}): Tarea {
  // Tarea base asignada a Luis
  return {
    id: 10, titulo: 'Diseñar la base de datos', descripcion: null, cursoId: null, cursoNombre: null, grupoId: 7, grupoNombre: 'Orvian Team',
    responsableId: 2, responsableNombre: 'Luis Rojas', estado: 'PENDIENTE', etiqueta: 'PROYECTO', fechaVencimiento: null, progreso: 0,
    fechaCreacion: '2026-11-01T10:00:00Z', ...cambios,
  };
}

// Crea una página de tareas como la que devuelve la API
function pagina(items: Tarea[]): Pagina<Tarea> {
  // Página con los elementos indicados
  return { content: items, page: { size: 20, number: 0, totalElements: items.length, totalPages: 1 } };
}

// Pruebas del detalle de un grupo
describe('GrupoDetallePage', () => {
  // Controlador de peticiones simuladas
  let http: HttpTestingController;
  // Componente en prueba
  let fixture: ComponentFixture<GrupoDetallePage>;
  // Raíz del componente
  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;

  // Configura Angular antes de cada prueba, con una sesión del usuario 1
  beforeEach(async () => {
    // Deja una sesión vigente guardada
    sessionStorage.setItem('orvian.sesion', JSON.stringify({ token: crearToken(3600), usuario: USUARIO_PRUEBA }));
    // Configura el componente, las rutas y el cliente HTTP de pruebas
    await TestBed.configureTestingModule({ imports: [GrupoDetallePage], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    // Obtiene el controlador de peticiones
    http = TestBed.inject(HttpTestingController);
    // Crea el componente
    fixture = TestBed.createComponent(GrupoDetallePage);
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

  // Busca la petición de las tareas del grupo
  function peticionDeTareas(): TestRequest {
    // La consulta de tareas del grupo 7
    return http.expectOne((peticion) => peticion.method === 'GET' && peticion.url.endsWith('/api/grupos/7/tareas'));
  }

  // Abre el detalle del grupo 7 y responde a las consultas iniciales
  function abrir(miRol: RolGrupo, tareas: Tarea[] = []): void {
    // La dirección entrega el identificador
    fixture.componentRef.setInput('id', '7');
    // Dispara la carga inicial
    fixture.detectChanges();
    // Responde con el grupo
    http.expectOne((peticion) => peticion.method === 'GET' && peticion.url.endsWith('/api/grupos/7')).flush(crearDetalle(miRol));
    // Responde con los cursos
    http.expectOne((peticion) => peticion.url.endsWith('/api/cursos')).flush([]);
    // Responde con las tareas
    peticionDeTareas().flush(pagina(tareas));
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

  // Etiquetas de accesibilidad de todos los botones
  function etiquetas(): string[] {
    // Lee el atributo de cada botón
    return Array.from(raiz().querySelectorAll('button')).map((candidato) => candidato.getAttribute('aria-label') ?? '');
  }

  // Textos de todos los botones
  function textos(): string[] {
    // Lee el texto de cada botón
    return Array.from(raiz().querySelectorAll('button')).map((candidato) => candidato.textContent?.trim() ?? '');
  }

  // Vista del líder
  it('al líder le muestra invitar, quitar integrantes y eliminar el grupo', () => {
    // Abre como líder
    abrir('LIDER');
    // Hay formulario de invitación
    expect(raiz().querySelector('input[aria-label="Correo de la persona a invitar"]')).not.toBeNull();
    // Puede quitar a Luis
    expect(etiquetas()).toContain('Quitar a Luis Rojas');
    // Puede eliminar el grupo
    expect(textos()).toContain('Eliminar grupo');
    // No puede abandonarlo
    expect(textos()).not.toContain('Abandonar grupo');
    // Tampoco puede quitarse a sí mismo
    expect(etiquetas()).not.toContain('Quitar a Ana Pérez');
  });

  // Vista del miembro
  it('al miembro le muestra abandonar y nada de administración', () => {
    // Abre como miembro
    abrir('MIEMBRO');
    // Puede abandonar
    expect(textos()).toContain('Abandonar grupo');
    // No hay formulario de invitación
    expect(raiz().querySelector('input[aria-label="Correo de la persona a invitar"]')).toBeNull();
    // No puede eliminar el grupo
    expect(textos()).not.toContain('Eliminar grupo');
    // No puede quitar integrantes
    expect(etiquetas().some((etiqueta) => etiqueta.startsWith('Quitar a'))).toBe(false);
  });

  // Invitar
  it('invita por correo y recarga los integrantes', () => {
    // Abre como líder
    abrir('LIDER');
    // Escribe el correo
    const campo = raiz().querySelector<HTMLInputElement>('input[aria-label="Correo de la persona a invitar"]')!;
    campo.value = ' maria@correo.com ';
    campo.dispatchEvent(new Event('input'));
    // Envía el formulario
    raiz().querySelector('form')!.dispatchEvent(new Event('submit'));
    // La petición lleva el correo sin espacios sobrantes
    const invitacion = http.expectOne((peticion) => peticion.method === 'POST' && peticion.url.endsWith('/api/grupos/7/integrantes'));
    expect(invitacion.request.body).toEqual({ correo: 'maria@correo.com' });
    // La API responde con el nuevo integrante
    invitacion.flush({ usuarioId: 3, nombre: 'María', carrera: null, ciclo: null, rolGrupo: 'MIEMBRO', fechaIngreso: '2026-11-02T10:00:00Z' });
    // Se recarga el grupo
    http.expectOne((peticion) => peticion.method === 'GET' && peticion.url.endsWith('/api/grupos/7')).flush(crearDetalle('LIDER'));
  });

  // Quitar a alguien
  it('pide confirmación antes de quitar a un integrante', () => {
    // Abre como líder
    abrir('LIDER');
    // Pulsa quitar a Luis
    raiz().querySelector<HTMLButtonElement>('button[aria-label="Quitar a Luis Rojas"]')!.click();
    // Actualiza la pantalla
    fixture.detectChanges();
    // Todavía no se envió nada
    http.expectNone((peticion) => peticion.method === 'DELETE');
    // Confirma
    boton('Sí, quitar').click();
    // Se envía la orden de quitar
    http.expectOne((peticion) => peticion.method === 'DELETE' && peticion.url.endsWith('/api/grupos/7/integrantes/2')).flush(null, { status: 204, statusText: 'No Content' });
    // Se recarga el grupo
    http.expectOne((peticion) => peticion.method === 'GET' && peticion.url.endsWith('/api/grupos/7')).flush(crearDetalle('LIDER'));
  });

  // Abandonar
  it('el miembro abandona el grupo con su propio identificador y vuelve a la lista', () => {
    // Espía la navegación
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    // Abre como miembro
    abrir('MIEMBRO');
    // Pulsa abandonar y confirma
    boton('Abandonar grupo').click();
    fixture.detectChanges();
    boton('Sí, abandonar').click();
    // Se envía la orden con el identificador propio, que es el 1
    http.expectOne((peticion) => peticion.method === 'DELETE' && peticion.url.endsWith('/api/grupos/7/integrantes/1')).flush(null, { status: 204, statusText: 'No Content' });
    // Vuelve a la lista de grupos
    expect(navegar).toHaveBeenCalledWith(['/grupos']);
  });

  // Eliminar el grupo
  it('el líder elimina el grupo tras confirmar y vuelve a la lista', () => {
    // Espía la navegación
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    // Abre como líder
    abrir('LIDER');
    // Pulsa eliminar y confirma
    boton('Eliminar grupo').click();
    fixture.detectChanges();
    boton('Sí, eliminar grupo').click();
    // Se envía la orden de eliminar
    http.expectOne((peticion) => peticion.method === 'DELETE' && peticion.url.endsWith('/api/grupos/7')).flush(null, { status: 204, statusText: 'No Content' });
    // Vuelve a la lista de grupos
    expect(navegar).toHaveBeenCalledWith(['/grupos']);
  });

  // Permisos sobre las tareas: miembro
  it('el miembro solo puede avanzar sus propias tareas y no puede editarlas ni borrarlas', () => {
    // Abre como miembro con una tarea propia y una ajena
    abrir('MIEMBRO', [crearTarea({ id: 10, responsableId: 1, titulo: 'La mía' }), crearTarea({ id: 11, responsableId: 2, titulo: 'La de Luis' })]);
    // Solo hay un botón Empezar: el de su tarea
    expect(textos().filter((texto) => texto === 'Empezar')).toHaveLength(1);
    // No hay botones de editar ni de borrar tareas
    expect(etiquetas().some((etiqueta) => etiqueta.startsWith('Editar') || etiqueta.startsWith('Borrar'))).toBe(false);
  });

  // Permisos sobre las tareas: líder
  it('el líder puede avanzar, editar y borrar cualquier tarea', () => {
    // Abre como líder con una tarea de otra persona
    abrir('LIDER', [crearTarea({ responsableId: 2 })]);
    // Puede empezarla
    expect(textos()).toContain('Empezar');
    // Puede editarla
    expect(etiquetas()).toContain('Editar Diseñar la base de datos');
    // Puede borrarla
    expect(etiquetas()).toContain('Borrar Diseñar la base de datos');
  });

  // Avance de una tarea
  it('al empezar una tarea envía el avance inicial a la ruta del grupo y recarga', () => {
    // Abre como miembro con una tarea propia
    abrir('MIEMBRO', [crearTarea({ responsableId: 1 })]);
    // Pulsa Empezar
    boton('Empezar').click();
    // La petición va a la ruta de tareas del grupo
    const cambio = http.expectOne((peticion) => peticion.method === 'PATCH' && peticion.url.endsWith('/api/grupos/7/tareas/10/avance'));
    // Con el avance inicial
    expect(cambio.request.body).toEqual({ estado: 'EN_PROGRESO', progreso: 10 });
    // La API responde
    cambio.flush(crearTarea({ estado: 'EN_PROGRESO', progreso: 10 }));
    // Se recarga la lista
    peticionDeTareas().flush(pagina([crearTarea({ estado: 'EN_PROGRESO', progreso: 10, responsableId: 1 })]));
  });

  // Asignar una tarea
  it('el líder crea una tarea asignada a otro integrante', () => {
    // Abre como líder
    abrir('LIDER');
    // Abre el diálogo
    boton('Nueva tarea').click();
    fixture.detectChanges();
    // Escribe el título
    const titulo = raiz().querySelector<HTMLInputElement>('#titulo')!;
    titulo.value = 'Preparar la demo';
    titulo.dispatchEvent(new Event('input'));
    // Elige a Luis: las opciones son Yo, Ana y Luis
    const responsable = raiz().querySelector<HTMLSelectElement>('#responsable')!;
    responsable.selectedIndex = 2;
    responsable.dispatchEvent(new Event('change'));
    // Envía el formulario
    raiz().querySelector('app-tarea-formulario form')!.dispatchEvent(new Event('submit'));
    // La petición va a la ruta del grupo con el responsable elegido
    const creacion = http.expectOne((peticion) => peticion.method === 'POST' && peticion.url.endsWith('/api/grupos/7/tareas'));
    expect(creacion.request.body).toMatchObject({ titulo: 'Preparar la demo', responsableId: 2 });
    // La API responde con la tarea creada
    creacion.flush(crearTarea({ titulo: 'Preparar la demo' }));
    // Se recarga la lista
    peticionDeTareas().flush(pagina([]));
  });

  // El miembro no puede asignar
  it('al miembro le bloquea el campo para asignar a otras personas', () => {
    // Abre como miembro
    abrir('MIEMBRO');
    // Abre el diálogo
    boton('Nueva tarea').click();
    fixture.detectChanges();
    // El campo de asignación está bloqueado
    expect(raiz().querySelector<HTMLSelectElement>('#responsable')!.disabled).toBe(true);
  });

  // Grupo ajeno
  it('muestra un mensaje si el grupo no existe o no pertenece al usuario', () => {
    // La dirección entrega el identificador
    fixture.componentRef.setInput('id', '7');
    // Dispara la carga inicial
    fixture.detectChanges();
    // La API responde como si no existiera
    http.expectOne((peticion) => peticion.method === 'GET' && peticion.url.endsWith('/api/grupos/7')).flush({ title: 'No encontrado', detail: 'Grupo no encontrado' }, { status: 404, statusText: 'Not Found' });
    // Los cursos se piden igual
    http.expectOne((peticion) => peticion.url.endsWith('/api/cursos')).flush([]);
    // Actualiza la pantalla
    fixture.detectChanges();
    // Se muestra el mensaje y el enlace para volver
    expect(raiz().textContent).toContain('Grupo no encontrado');
    expect(raiz().textContent).toContain('Volver a mis grupos');
  });
});
