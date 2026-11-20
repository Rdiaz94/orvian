// Formatos de fecha
import { DatePipe } from '@angular/common';
// Utilidades de Angular
import { Component, OnInit, inject, signal } from '@angular/core';

// Servicios de la API
import { CursoApi } from '../../core/api/curso.service';
import { TareaApi } from '../../core/api/tarea.service';
// Conversión de errores en mensajes
import { mensajeDeError } from '../../core/errores';
// Utilidad de fechas
import { estaVencida } from '../../core/fechas';
// Tipos de datos
import { Curso, EstadoTarea, Tarea } from '../../core/modelos';
// Diálogo para crear y editar
import { TareaFormulario } from './tarea-formulario';

// Tamaño de cada página de tareas
const TAMANO_PAGINA = 20;
// Avance inicial al empezar una tarea
const AVANCE_INICIAL = 10;

// Columna del tablero
interface Columna {
  // Estado que representa
  estado: EstadoTarea;
  // Título que se muestra
  titulo: string;
  // Ícono de Bootstrap Icons
  icono: string;
  // Color del recuadro del ícono
  color: string;
  // Tareas cargadas
  items: Tarea[];
  // Total de tareas en ese estado, incluidas las no cargadas
  total: number;
  // Última página cargada
  pagina: number;
  // Indica si se están cargando datos
  cargando: boolean;
}

// Columnas iniciales, en el orden en que se muestran
const COLUMNAS_INICIALES: Columna[] = [
  // Tareas sin empezar
  { estado: 'PENDIENTE', titulo: 'Pendientes', icono: 'bi-hourglass-split', color: 'or-azul', items: [], total: 0, pagina: 0, cargando: true },
  // Tareas que se están trabajando
  { estado: 'EN_PROGRESO', titulo: 'En progreso', icono: 'bi-lightning-charge-fill', color: 'or-ambar', items: [], total: 0, pagina: 0, cargando: true },
  // Tareas terminadas
  { estado: 'HECHA', titulo: 'Hechas', icono: 'bi-check2-circle', color: 'or-morado', items: [], total: 0, pagina: 0, cargando: true },
];

// Tablero de tareas por estado
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-tareas',
  // Componentes y utilidades que usa su plantilla
  imports: [DatePipe, TareaFormulario],
  // Archivo de la plantilla
  templateUrl: './tareas.html',
  // Archivo de los estilos
  styleUrl: './tareas.scss',
})
export class TareasPage implements OnInit {
  // Servicio de tareas
  private readonly api = inject(TareaApi);
  // Servicio de cursos
  private readonly cursoApi = inject(CursoApi);

  // Columnas del tablero
  protected readonly columnas = signal<Columna[]>(COLUMNAS_INICIALES);
  // Cursos disponibles para el formulario
  protected readonly cursos = signal<Curso[]>([]);
  // Mensaje de error, o nulo
  protected readonly error = signal<string | null>(null);
  // Indica si el diálogo está abierto
  protected readonly dialogoAbierto = signal(false);
  // Tarea que se edita, o nulo si se crea una nueva
  protected readonly enEdicion = signal<Tarea | null>(null);
  // Identificador de la tarea que espera confirmación de borrado
  protected readonly confirmandoId = signal<number | null>(null);
  // Tarea que se está arrastrando
  protected readonly arrastrando = signal<Tarea | null>(null);

  // Carga los datos al abrir la pantalla
  ngOnInit(): void {
    // Carga los cursos para el formulario
    this.cursoApi.listar().subscribe({ next: (cursos) => this.cursos.set(cursos), error: (error: unknown) => this.error.set(mensajeDeError(error)) });
    // Carga cada columna
    this.recargar(['PENDIENTE', 'EN_PROGRESO', 'HECHA']);
  }

  // Vuelve a cargar desde la primera página las columnas indicadas
  protected recargar(estados: EstadoTarea[]): void {
    // Repite para cada estado
    estados.forEach((estado) => this.cargarColumna(estado, 0));
  }

  // Carga una página de una columna; la primera reemplaza el contenido y las siguientes se agregan
  protected cargarColumna(estado: EstadoTarea, pagina: number): void {
    // Muestra el estado de carga de la columna
    this.actualizarColumna(estado, { cargando: true });
    // Consulta la API
    this.api.listar(estado, pagina, TAMANO_PAGINA).subscribe({
      // Si salió bien, guarda los datos
      next: (resultado) => {
        // Tareas actuales de la columna
        const actuales = this.columnas().find((columna) => columna.estado === estado)?.items ?? [];
        // Actualiza la columna
        this.actualizarColumna(estado, {
          // La primera página reemplaza; las demás se agregan
          items: pagina === 0 ? resultado.content : [...actuales, ...resultado.content],
          // Total de tareas en ese estado
          total: resultado.page.totalElements,
          // Página cargada
          pagina,
          // Termina la carga
          cargando: false,
        });
      },
      // Si falló, muestra el motivo
      error: (error: unknown) => {
        // Guarda el mensaje
        this.error.set(mensajeDeError(error));
        // Termina la carga
        this.actualizarColumna(estado, { cargando: false });
      },
    });
  }

  // Indica si una columna tiene más tareas sin cargar
  protected hayMas(columna: Columna): boolean {
    // Hay más si lo cargado es menor que el total
    return columna.items.length < columna.total;
  }

  // Empieza una tarea pendiente, conservando el avance si ya tenía uno intermedio
  protected iniciar(tarea: Tarea): void {
    // Avance intermedio que ya tenía, o el inicial
    const avance = tarea.progreso >= 1 && tarea.progreso <= 99 ? tarea.progreso : AVANCE_INICIAL;
    // Mueve la tarea
    this.mover(tarea, 'EN_PROGRESO', avance);
  }

  // Marca una tarea como hecha
  protected terminar(tarea: Tarea): void {
    // Mueve la tarea
    this.mover(tarea, 'HECHA', null);
  }

  // Devuelve una tarea hecha a pendiente
  protected reabrir(tarea: Tarea): void {
    // Mueve la tarea
    this.mover(tarea, 'PENDIENTE', null);
  }

  // Cambia el avance de una tarea en progreso con el control deslizante
  protected cambiarAvance(tarea: Tarea, evento: Event): void {
    // Valor elegido en el control
    const valor = Number((evento.target as HTMLInputElement).value);
    // Envía el cambio a la API
    this.api.registrarAvance(tarea.id, { estado: 'EN_PROGRESO', progreso: valor }).subscribe({
      // Reemplaza la tarea con la versión actualizada
      next: (actualizada) => this.reemplazar(actualizada),
      // Si falló, muestra el motivo y recupera el valor real
      error: (error: unknown) => {
        // Guarda el mensaje
        this.error.set(mensajeDeError(error));
        // Recarga la columna
        this.recargar(['EN_PROGRESO']);
      },
    });
  }

  // Mueve una tarea a otro estado
  protected mover(tarea: Tarea, destino: EstadoTarea, progreso: number | null): void {
    // Si ya está en ese estado, no hay nada que hacer
    if (tarea.estado === destino) {
      // Termina
      return;
    }
    // Borra el error anterior
    this.error.set(null);
    // Envía el cambio a la API
    this.api.registrarAvance(tarea.id, { estado: destino, progreso }).subscribe({
      // Si salió bien, recarga las dos columnas afectadas
      next: () => this.recargar([tarea.estado, destino]),
      // Si falló, muestra el motivo
      error: (error: unknown) => this.error.set(mensajeDeError(error)),
    });
  }

  // Registra la tarea que empieza a arrastrarse
  protected alArrastrar(tarea: Tarea): void {
    // Guarda la tarea
    this.arrastrando.set(tarea);
  }

  // Permite soltar sobre una columna
  protected permitirSoltar(evento: DragEvent): void {
    // Sin esto el navegador no admite soltar
    evento.preventDefault();
  }

  // Mueve la tarea arrastrada a la columna donde se soltó
  protected alSoltar(evento: DragEvent, destino: EstadoTarea): void {
    // Evita el comportamiento por defecto del navegador
    evento.preventDefault();
    // Tarea arrastrada
    const tarea = this.arrastrando();
    // Termina el arrastre
    this.arrastrando.set(null);
    // Si no hay tarea, no hay nada que mover
    if (!tarea) {
      // Termina
      return;
    }
    // Una tarea que se suelta en progreso conserva su avance intermedio o empieza con el inicial
    const progreso = destino === 'EN_PROGRESO' ? (tarea.progreso >= 1 && tarea.progreso <= 99 ? tarea.progreso : AVANCE_INICIAL) : null;
    // Mueve la tarea
    this.mover(tarea, destino, progreso);
  }

  // Abre el diálogo para crear una tarea
  protected abrirNueva(): void {
    // No hay tarea en edición
    this.enEdicion.set(null);
    // Abre el diálogo
    this.dialogoAbierto.set(true);
  }

  // Abre el diálogo para editar una tarea
  protected abrirEdicion(tarea: Tarea): void {
    // Guarda la tarea en edición
    this.enEdicion.set(tarea);
    // Abre el diálogo
    this.dialogoAbierto.set(true);
  }

  // Cierra el diálogo sin guardar
  protected cerrarDialogo(): void {
    // Cierra el diálogo
    this.dialogoAbierto.set(false);
  }

  // Cierra el diálogo y recarga el tablero después de guardar
  protected alGuardar(): void {
    // Cierra el diálogo
    this.dialogoAbierto.set(false);
    // La fecha o el estado pueden cambiar el orden, así que se recarga todo
    this.recargar(['PENDIENTE', 'EN_PROGRESO', 'HECHA']);
  }

  // Pide confirmación antes de borrar
  protected pedirBorrado(tarea: Tarea): void {
    // Marca la tarea que espera confirmación
    this.confirmandoId.set(tarea.id);
  }

  // Cancela el borrado
  protected cancelarBorrado(): void {
    // Quita la marca
    this.confirmandoId.set(null);
  }

  // Borra una tarea personal
  protected borrar(tarea: Tarea): void {
    // Quita la marca de confirmación
    this.confirmandoId.set(null);
    // Envía la orden a la API
    this.api.eliminar(tarea.id).subscribe({
      // Si salió bien, recarga su columna
      next: () => this.recargar([tarea.estado]),
      // Si falló, muestra el motivo
      error: (error: unknown) => this.error.set(mensajeDeError(error)),
    });
  }

  // Indica si una tarea sin terminar ya pasó su fecha límite
  protected vencida(tarea: Tarea): boolean {
    // Solo cuentan las tareas sin terminar
    return tarea.estado !== 'HECHA' && estaVencida(tarea.fechaVencimiento);
  }

  // Reemplaza una tarea por su versión actualizada dentro de su columna
  private reemplazar(actualizada: Tarea): void {
    // Actualiza la columna de la tarea
    this.actualizarColumna(actualizada.estado, {
      // Cambia solo la tarea que coincide
      items: (this.columnas().find((columna) => columna.estado === actualizada.estado)?.items ?? []).map((tarea) => (tarea.id === actualizada.id ? actualizada : tarea)),
    });
  }

  // Cambia algunos datos de una columna sin alterar las demás
  private actualizarColumna(estado: EstadoTarea, cambios: Partial<Columna>): void {
    // Crea una lista nueva para que Angular detecte el cambio
    this.columnas.update((columnas) => columnas.map((columna) => (columna.estado === estado ? { ...columna, ...cambios } : columna)));
  }
}
