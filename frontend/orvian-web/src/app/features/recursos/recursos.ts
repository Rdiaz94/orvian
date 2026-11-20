// Utilidades de Angular
import { Component, OnInit, effect, inject, input, signal, untracked } from '@angular/core';
// Navegación entre pantallas
import { Router } from '@angular/router';

// Servicios de la API
import { CursoApi } from '../../core/api/curso.service';
import { GrupoApi } from '../../core/api/grupo.service';
import { RecursoApi } from '../../core/api/recurso.service';
// Validación de enlaces
import { esUrlSegura } from '../../core/enlaces';
// Conversión de errores en mensajes
import { mensajeDeError } from '../../core/errores';
// Tipos de datos
import { Curso, Grupo, OrdenRecurso, Recurso, TipoRecurso } from '../../core/modelos';
// Componentes de la pantalla
import { RecursoFormulario } from './recurso-formulario';
import { RecursoTarjeta } from './recurso-tarjeta';

// Cantidad de recursos por página
const TAMANO_PAGINA = 12;
// Cantidad de recursos del Top 10
const TAMANO_TOP = 10;

// Biblioteca de recursos con búsqueda, filtros, Top 10 y gestión de los recursos propios
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-recursos',
  // Componentes que usa su plantilla
  imports: [RecursoTarjeta, RecursoFormulario],
  // Archivo de la plantilla
  templateUrl: './recursos.html',
  // Archivo de los estilos
  styleUrl: './recursos.scss',
})
export class RecursosPage implements OnInit {
  // Texto de búsqueda que llega por la dirección (?q=...); el buscador del menú también navega aquí
  readonly q = input<string | undefined>(undefined);

  // Servicio de recursos
  private readonly api = inject(RecursoApi);
  // Servicio de cursos
  private readonly cursoApi = inject(CursoApi);
  // Servicio de grupos
  private readonly grupoApi = inject(GrupoApi);
  // Navegador de pantallas
  private readonly router = inject(Router);

  // Tipos que se pueden filtrar
  protected readonly tipos: TipoRecurso[] = ['PDF', 'VIDEO', 'DOCUMENTO', 'ENLACE', 'QUIZ'];
  // Tipo elegido en el filtro, o nulo para todos
  protected readonly tipo = signal<TipoRecurso | null>(null);
  // Orden elegido
  protected readonly orden = signal<OrdenRecurso>('RECIENTES');
  // Texto escrito en el buscador de la pantalla
  protected readonly texto = signal('');
  // Recursos de la lista principal
  protected readonly items = signal<Recurso[]>([]);
  // Total de recursos que cumplen el filtro
  protected readonly total = signal(0);
  // Última página cargada
  protected readonly pagina = signal(0);
  // Indica si se está cargando la lista principal
  protected readonly cargando = signal(true);
  // Recursos más descargados
  protected readonly populares = signal<Recurso[]>([]);
  // Mensaje de error, o nulo
  protected readonly error = signal<string | null>(null);
  // Cursos para el formulario
  protected readonly cursos = signal<Curso[]>([]);
  // Grupos para el formulario
  protected readonly grupos = signal<Grupo[]>([]);
  // Indica si el diálogo está abierto
  protected readonly dialogoAbierto = signal(false);
  // Recurso que se edita, o nulo si se comparte uno nuevo
  protected readonly enEdicion = signal<Recurso | null>(null);

  // Indica si ya se pidieron los cursos y los grupos
  private opcionesCargadas = false;

  // Recarga la lista cada vez que cambia el texto de la dirección, incluida la primera lectura
  constructor() {
    // Efecto que reacciona al parámetro q
    effect(() => {
      // Texto recibido por la dirección
      const recibido = this.q() ?? '';
      // El resto se ejecuta sin que el efecto dependa de otras señales
      untracked(() => {
        // Refleja el texto en el buscador
        this.texto.set(recibido);
        // Carga la primera página con el nuevo texto
        this.cargar(0);
      });
    });
  }

  // Carga el Top 10 al abrir la pantalla
  ngOnInit(): void {
    // Pide los recursos más descargados
    this.cargarPopulares();
  }

  // Indica si hay filtros o búsqueda activos
  protected hayFiltros(): boolean {
    // Hay filtros si se eligió un tipo o se escribió texto
    return this.tipo() !== null || this.texto().trim() !== '';
  }

  // Indica si quedan recursos sin cargar
  protected hayMas(): boolean {
    // Hay más si lo cargado es menor que el total
    return this.items().length < this.total();
  }

  // Carga una página de la lista principal; la primera reemplaza el contenido y las demás se agregan
  protected cargar(pagina: number): void {
    // Muestra el estado de carga
    this.cargando.set(true);
    // Consulta la API con los filtros actuales
    this.api.listar({ tipo: this.tipo() ?? undefined, texto: this.q()?.trim() || undefined, orden: this.orden() }, pagina, TAMANO_PAGINA).subscribe({
      // Si salió bien, guarda los datos
      next: (resultado) => {
        // La primera página reemplaza; las demás se agregan
        this.items.update((actuales) => (pagina === 0 ? resultado.content : [...actuales, ...resultado.content]));
        // Total de recursos
        this.total.set(resultado.page.totalElements);
        // Página cargada
        this.pagina.set(pagina);
        // Termina la carga
        this.cargando.set(false);
      },
      // Si falló, muestra el motivo
      error: (error: unknown) => {
        // Guarda el mensaje
        this.error.set(mensajeDeError(error));
        // Termina la carga
        this.cargando.set(false);
      },
    });
  }

  // Carga los recursos más descargados
  protected cargarPopulares(): void {
    // Consulta la API
    this.api.listar({ orden: 'POPULARES' }, 0, TAMANO_TOP).subscribe({
      // Si salió bien, guarda los datos
      next: (resultado) => this.populares.set(resultado.content),
      // Si falló, muestra el motivo
      error: (error: unknown) => this.error.set(mensajeDeError(error)),
    });
  }

  // Cambia el filtro de tipo
  protected cambiarTipo(tipo: TipoRecurso | null): void {
    // Guarda el tipo elegido
    this.tipo.set(tipo);
    // Recarga desde la primera página
    this.cargar(0);
  }

  // Cambia el orden de la lista
  protected cambiarOrden(orden: OrdenRecurso): void {
    // Guarda el orden elegido
    this.orden.set(orden);
    // Recarga desde la primera página
    this.cargar(0);
  }

  // Guarda lo que se escribe en el buscador
  protected alEscribir(evento: Event): void {
    // Campo de texto que originó el evento
    const campo = evento.target as HTMLInputElement;
    // Actualiza el texto
    this.texto.set(campo.value);
  }

  // Lleva la búsqueda a la dirección, que dispara la recarga
  protected buscar(evento: Event): void {
    // Evita que el formulario recargue la página
    evento.preventDefault();
    // Texto sin espacios sobrantes
    const texto = this.texto().trim();
    // Navega a la misma pantalla con el texto como parámetro
    void this.router.navigate(['/recursos'], { queryParams: texto ? { q: texto } : {} });
  }

  // Abre el recurso: registra la descarga y abre el enlace solo si es http o https
  protected abrir(recurso: Recurso): void {
    // Borra el error anterior
    this.error.set(null);
    // Registra la descarga en la API
    this.api.registrarDescarga(recurso.id).subscribe({
      // Si salió bien, abre el enlace
      next: (respuesta) => {
        // Un enlace que no sea web nunca se abre
        if (!esUrlSegura(respuesta.url)) {
          // Avisa del problema
          this.error.set('El enlace de este recurso no es seguro y no se abrió.');
          // Termina
          return;
        }
        // Abre el enlace en otra pestaña sin darle acceso a esta
        window.open(respuesta.url, '_blank', 'noopener,noreferrer');
        // Refleja la descarga en pantalla sin volver a consultar
        this.sumarDescarga(recurso.id);
      },
      // Si falló, muestra el motivo
      error: (error: unknown) => this.error.set(mensajeDeError(error)),
    });
  }

  // Borra un recurso propio
  protected borrar(recurso: Recurso): void {
    // Envía la orden a la API
    this.api.eliminar(recurso.id).subscribe({
      // Si salió bien, recarga las listas
      next: () => this.recargarTodo(),
      // Si falló, muestra el motivo
      error: (error: unknown) => this.error.set(mensajeDeError(error)),
    });
  }

  // Abre el diálogo para compartir un recurso nuevo
  protected abrirNuevo(): void {
    // No hay recurso en edición
    this.enEdicion.set(null);
    // Carga los cursos y los grupos si hace falta
    this.cargarOpciones();
    // Abre el diálogo
    this.dialogoAbierto.set(true);
  }

  // Abre el diálogo para editar un recurso
  protected abrirEdicion(recurso: Recurso): void {
    // Guarda el recurso en edición
    this.enEdicion.set(recurso);
    // Carga los cursos y los grupos si hace falta
    this.cargarOpciones();
    // Abre el diálogo
    this.dialogoAbierto.set(true);
  }

  // Cierra el diálogo sin guardar
  protected cerrarDialogo(): void {
    // Cierra el diálogo
    this.dialogoAbierto.set(false);
  }

  // Cierra el diálogo y recarga las listas después de guardar
  protected alGuardar(): void {
    // Cierra el diálogo
    this.dialogoAbierto.set(false);
    // Recarga las listas
    this.recargarTodo();
  }

  // Vuelve a cargar la lista principal y el Top 10
  private recargarTodo(): void {
    // Lista principal desde la primera página
    this.cargar(0);
    // Top 10
    this.cargarPopulares();
  }

  // Pide los cursos y los grupos una sola vez
  private cargarOpciones(): void {
    // Si ya se pidieron, no se repite
    if (this.opcionesCargadas) {
      // Termina
      return;
    }
    // Marca que ya se pidieron
    this.opcionesCargadas = true;
    // Cursos disponibles
    this.cursoApi.listar().subscribe({ next: (cursos) => this.cursos.set(cursos), error: (error: unknown) => this.error.set(mensajeDeError(error)) });
    // Grupos del usuario
    this.grupoApi.listar(0, 50).subscribe({ next: (pagina) => this.grupos.set(pagina.content), error: (error: unknown) => this.error.set(mensajeDeError(error)) });
  }

  // Suma una descarga al recurso en las listas que se muestran
  private sumarDescarga(id: number): void {
    // Suma uno al recurso indicado, sin tocar los demás
    const sumar = (lista: Recurso[]): Recurso[] => lista.map((recurso) => (recurso.id === id ? { ...recurso, descargas: recurso.descargas + 1 } : recurso));
    // Actualiza la lista principal
    this.items.update(sumar);
    // Actualiza el Top 10
    this.populares.update(sumar);
  }
}
