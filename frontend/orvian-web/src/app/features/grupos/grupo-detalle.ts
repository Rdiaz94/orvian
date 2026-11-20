// Formatos de fecha
import { DatePipe } from '@angular/common';
// Utilidades de Angular
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
// Formularios reactivos
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
// Navegación entre pantallas
import { Router, RouterLink } from '@angular/router';

// Servicios de la API
import { CursoApi } from '../../core/api/curso.service';
import { GrupoApi } from '../../core/api/grupo.service';
import { TareaGrupoApi } from '../../core/api/tarea-grupo.service';
// Servicio de sesión
import { AuthService } from '../../core/auth.service';
// Conversión de errores en mensajes
import { mensajeDeError } from '../../core/errores';
// Utilidad de fechas
import { estaVencida } from '../../core/fechas';
// Tipos de datos
import { Curso, EstadoTarea, GrupoDetalle, Integrante, Tarea } from '../../core/modelos';
// Diálogo para crear y editar tareas
import { TareaFormulario } from '../tareas/tarea-formulario';

// Cantidad de tareas por página
const TAMANO_PAGINA = 20;
// Avance inicial al empezar una tarea
const AVANCE_INICIAL = 10;

// Qué se está confirmando: el borrado del grupo, la salida propia, la expulsión de alguien o el borrado de una tarea
type Confirmacion = { tipo: 'grupo' } | { tipo: 'abandono' } | { tipo: 'integrante'; id: number } | { tipo: 'tarea'; id: number };

// Detalle de un grupo: integrantes, invitaciones y tareas compartidas
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-grupo-detalle',
  // Componentes y utilidades que usa su plantilla
  imports: [DatePipe, ReactiveFormsModule, RouterLink, TareaFormulario],
  // Archivo de la plantilla
  templateUrl: './grupo-detalle.html',
})
export class GrupoDetallePage {
  // Identificador del grupo, tomado de la dirección /grupos/:id
  readonly id = input.required<string>();

  // Constructor de formularios con campos que nunca son nulos
  private readonly fb = inject(NonNullableFormBuilder);
  // Servicio de grupos
  private readonly api = inject(GrupoApi);
  // Servicio de tareas de grupo
  private readonly tareaApi = inject(TareaGrupoApi);
  // Servicio de cursos
  private readonly cursoApi = inject(CursoApi);
  // Servicio de sesión
  private readonly auth = inject(AuthService);
  // Navegador de pantallas
  private readonly router = inject(Router);

  // Datos del grupo, o nulo mientras cargan
  protected readonly detalle = signal<GrupoDetalle | null>(null);
  // Indica si se está cargando el grupo
  protected readonly cargando = signal(true);
  // Mensaje de error, o nulo
  protected readonly error = signal<string | null>(null);
  // Tareas del grupo cargadas
  protected readonly tareas = signal<Tarea[]>([]);
  // Total de tareas que cumplen el filtro
  protected readonly totalTareas = signal(0);
  // Última página de tareas cargada
  protected readonly paginaTareas = signal(0);
  // Estado elegido en el filtro de tareas, o nulo para todas
  protected readonly filtro = signal<EstadoTarea | null>(null);
  // Cursos para el formulario de tareas
  protected readonly cursos = signal<Curso[]>([]);
  // Indica si el diálogo de tareas está abierto
  protected readonly dialogoAbierto = signal(false);
  // Tarea que se edita, o nulo si se crea una nueva
  protected readonly enEdicion = signal<Tarea | null>(null);
  // Acción que espera confirmación, o nulo
  protected readonly confirmando = signal<Confirmacion | null>(null);
  // Indica si se está enviando una invitación
  protected readonly invitando = signal(false);

  // Estados que se pueden filtrar
  protected readonly estados: EstadoTarea[] = ['PENDIENTE', 'EN_PROGRESO', 'HECHA'];
  // Texto de cada estado
  protected readonly textoEstado: Record<EstadoTarea, string> = { PENDIENTE: 'Pendiente', EN_PROGRESO: 'En progreso', HECHA: 'Hecha' };
  // Color de la etiqueta de cada estado
  protected readonly claseEstado: Record<EstadoTarea, string> = { PENDIENTE: 'or-chip', EN_PROGRESO: 'or-chip or-chip-ambar', HECHA: 'or-chip or-chip-azul' };

  // Formulario de invitación
  protected readonly invitacion = this.fb.group({
    // El correo es obligatorio y debe tener formato válido
    correo: ['', [Validators.required, Validators.email]],
  });

  // Identificador numérico del grupo
  protected readonly grupoId = computed(() => Number(this.id()));
  // Identificador del usuario que consulta
  protected readonly miId = computed(() => this.auth.usuario()?.id ?? null);
  // Indica si el usuario es el líder del grupo
  protected readonly soyLider = computed(() => this.detalle()?.grupo.miRol === 'LIDER');
  // Integrantes del grupo
  protected readonly integrantes = computed<Integrante[]>(() => this.detalle()?.integrantes ?? []);

  // Carga el grupo cada vez que cambia el identificador de la dirección
  constructor() {
    // Efecto que reacciona al identificador
    effect(() => {
      // Identificador actual
      const id = this.grupoId();
      // El resto se ejecuta sin que el efecto dependa de otras señales
      untracked(() => this.cargarTodo(id));
    });
  }

  // Carga el grupo, sus tareas y los cursos
  protected cargarTodo(id: number): void {
    // Un identificador que no es número no existe
    if (!Number.isInteger(id)) {
      // Muestra el motivo
      this.error.set('Grupo no encontrado');
      // Termina la carga
      this.cargando.set(false);
      // Termina
      return;
    }
    // Muestra el estado de carga
    this.cargando.set(true);
    // Borra el error anterior
    this.error.set(null);
    // Consulta el grupo
    this.api.obtener(id).subscribe({
      // Si salió bien, guarda los datos y carga las tareas
      next: (detalle) => {
        // Guarda el detalle
        this.detalle.set(detalle);
        // Termina la carga
        this.cargando.set(false);
        // Carga la primera página de tareas
        this.cargarTareas(0);
      },
      // Si falló, muestra el motivo; un grupo ajeno responde igual que uno inexistente
      error: (error: unknown) => {
        // Guarda el mensaje
        this.error.set(mensajeDeError(error));
        // Termina la carga
        this.cargando.set(false);
      },
    });
    // Carga los cursos para el formulario de tareas
    this.cursoApi.listar().subscribe({ next: (cursos) => this.cursos.set(cursos), error: () => undefined });
  }

  // Recarga solo el grupo y sus integrantes
  protected recargarGrupo(): void {
    // Consulta el grupo
    this.api.obtener(this.grupoId()).subscribe({
      // Si salió bien, guarda los datos
      next: (detalle) => this.detalle.set(detalle),
      // Si falló, muestra el motivo
      error: (error: unknown) => this.error.set(mensajeDeError(error)),
    });
  }

  // Carga una página de tareas; la primera reemplaza el contenido y las demás se agregan
  protected cargarTareas(pagina: number): void {
    // Consulta la API con el filtro actual
    this.tareaApi.listar(this.grupoId(), this.filtro() ?? undefined, pagina, TAMANO_PAGINA).subscribe({
      // Si salió bien, guarda los datos
      next: (resultado) => {
        // La primera página reemplaza; las demás se agregan
        this.tareas.update((actuales) => (pagina === 0 ? resultado.content : [...actuales, ...resultado.content]));
        // Total de tareas
        this.totalTareas.set(resultado.page.totalElements);
        // Página cargada
        this.paginaTareas.set(pagina);
      },
      // Si falló, muestra el motivo
      error: (error: unknown) => this.error.set(mensajeDeError(error)),
    });
  }

  // Indica si quedan tareas sin cargar
  protected hayMasTareas(): boolean {
    // Hay más si lo cargado es menor que el total
    return this.tareas().length < this.totalTareas();
  }

  // Cambia el filtro de estado
  protected cambiarFiltro(estado: EstadoTarea | null): void {
    // Guarda el filtro
    this.filtro.set(estado);
    // Recarga desde la primera página
    this.cargarTareas(0);
  }

  // Indica si el usuario puede cambiar el avance de una tarea: la persona asignada o el líder
  protected puedeAvanzar(tarea: Tarea): boolean {
    // La persona asignada o el líder
    return this.soyLider() || tarea.responsableId === this.miId();
  }

  // Indica si una tarea sin terminar ya pasó su fecha límite
  protected vencida(tarea: Tarea): boolean {
    // Solo cuentan las tareas sin terminar
    return tarea.estado !== 'HECHA' && estaVencida(tarea.fechaVencimiento);
  }

  // Empieza una tarea conservando el avance intermedio que ya tuviera
  protected iniciar(tarea: Tarea): void {
    // Avance intermedio que ya tenía, o el inicial
    const avance = tarea.progreso >= 1 && tarea.progreso <= 99 ? tarea.progreso : AVANCE_INICIAL;
    // Envía el cambio
    this.cambiarEstado(tarea, 'EN_PROGRESO', avance);
  }

  // Cambia el estado de una tarea y recarga la lista
  protected cambiarEstado(tarea: Tarea, estado: EstadoTarea, progreso: number | null): void {
    // Borra el error anterior
    this.error.set(null);
    // Envía el cambio a la API
    this.tareaApi.registrarAvance(this.grupoId(), tarea.id, { estado, progreso }).subscribe({
      // Si salió bien, recarga la lista
      next: () => this.cargarTareas(0),
      // Si falló, muestra el motivo
      error: (error: unknown) => this.error.set(mensajeDeError(error)),
    });
  }

  // Cambia el avance de una tarea en progreso con el control deslizante
  protected cambiarAvance(tarea: Tarea, evento: Event): void {
    // Valor elegido en el control
    const valor = Number((evento.target as HTMLInputElement).value);
    // Envía el cambio
    this.cambiarEstado(tarea, 'EN_PROGRESO', valor);
  }

  // Abre el diálogo para crear una tarea
  protected abrirNueva(): void {
    // No hay tarea en edición
    this.enEdicion.set(null);
    // Abre el diálogo
    this.dialogoAbierto.set(true);
  }

  // Abre el diálogo para editar una tarea; solo el líder
  protected abrirEdicion(tarea: Tarea): void {
    // Guarda la tarea en edición
    this.enEdicion.set(tarea);
    // Abre el diálogo
    this.dialogoAbierto.set(true);
  }

  // Cierra el diálogo y recarga las tareas después de guardar
  protected alGuardar(): void {
    // Cierra el diálogo
    this.dialogoAbierto.set(false);
    // Recarga la lista
    this.cargarTareas(0);
  }

  // Invita a una persona por su correo
  protected invitar(): void {
    // Si el formulario es inválido, se muestran los errores y no se envía
    if (this.invitacion.invalid) {
      // Marca el campo para que muestre su error
      this.invitacion.markAllAsTouched();
      // Termina
      return;
    }
    // Muestra el estado de envío
    this.invitando.set(true);
    // Borra el error anterior
    this.error.set(null);
    // Envía la invitación
    this.api.agregarIntegrante(this.grupoId(), this.invitacion.getRawValue().correo.trim()).subscribe({
      // Si salió bien, limpia el formulario y recarga los integrantes
      next: () => {
        // Limpia el campo
        this.invitacion.reset();
        // Termina el envío
        this.invitando.set(false);
        // Recarga el grupo
        this.recargarGrupo();
      },
      // Si falló, muestra el motivo
      error: (error: unknown) => {
        // Guarda el mensaje
        this.error.set(mensajeDeError(error));
        // Termina el envío
        this.invitando.set(false);
      },
    });
  }

  // Ejecuta la acción que se confirmó
  protected confirmar(): void {
    // Acción pendiente
    const accion = this.confirmando();
    // Quita la marca de confirmación
    this.confirmando.set(null);
    // Si no había acción, no hay nada que hacer
    if (accion === null) {
      // Termina
      return;
    }
    // Borra el error anterior
    this.error.set(null);
    // Elige qué hacer según el tipo de acción
    switch (accion.tipo) {
      // Eliminar el grupo completo
      case 'grupo':
        // Envía la orden y vuelve a la lista de grupos
        this.api.eliminar(this.grupoId()).subscribe({ next: () => void this.router.navigate(['/grupos']), error: (error: unknown) => this.error.set(mensajeDeError(error)) });
        break;
      // Abandonar el grupo
      case 'abandono':
        // Quita al propio usuario y vuelve a la lista de grupos
        this.api.quitarIntegrante(this.grupoId(), this.miId() ?? 0).subscribe({ next: () => void this.router.navigate(['/grupos']), error: (error: unknown) => this.error.set(mensajeDeError(error)) });
        break;
      // Quitar a otro integrante
      case 'integrante':
        // Quita al integrante y recarga el grupo
        this.api.quitarIntegrante(this.grupoId(), accion.id).subscribe({ next: () => this.recargarGrupo(), error: (error: unknown) => this.error.set(mensajeDeError(error)) });
        break;
      // Borrar una tarea
      case 'tarea':
        // Borra la tarea y recarga la lista
        this.tareaApi.eliminar(this.grupoId(), accion.id).subscribe({ next: () => this.cargarTareas(0), error: (error: unknown) => this.error.set(mensajeDeError(error)) });
        break;
    }
  }

  // Indica si se está pidiendo confirmación para una acción concreta
  protected esperando(tipo: Confirmacion['tipo'], id?: number): boolean {
    // Acción pendiente
    const accion = this.confirmando();
    // Sin acción pendiente no se espera nada
    if (accion === null || accion.tipo !== tipo) {
      // No coincide
      return false;
    }
    // Las acciones sobre un elemento concreto también deben coincidir en su identificador
    return id === undefined || (accion.tipo !== 'grupo' && accion.tipo !== 'abandono' && accion.id === id);
  }
}
