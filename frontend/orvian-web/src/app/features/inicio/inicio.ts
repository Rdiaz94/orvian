// Formatos de fecha
import { DatePipe } from '@angular/common';
// Utilidades de Angular
import { Component, OnInit, computed, inject, signal } from '@angular/core';
// Enlaces entre pantallas
import { RouterLink } from '@angular/router';
// Operadores de flujos de datos
import { forkJoin, map } from 'rxjs';

// Servicio de sesión
import { AuthService } from '../../core/auth.service';
// Conversión de errores en mensajes
import { mensajeDeError } from '../../core/errores';
// Servicios de la API
import { GrupoApi } from '../../core/api/grupo.service';
import { RecursoApi } from '../../core/api/recurso.service';
import { ResumenApi } from '../../core/api/resumen.service';
import { TareaApi } from '../../core/api/tarea.service';
// Tipos de datos
import { EstadoTarea, EtiquetaTarea, Grupo, Recurso, ResumenAcademico, Tarea, TipoRecurso } from '../../core/modelos';

// Todo lo que necesita la pantalla, cargado en una sola vez
interface Panel {
  // Estadísticas del usuario
  resumen: ResumenAcademico;
  // Tareas que todavía no terminó
  tareas: Tarea[];
  // Sus grupos
  grupos: Grupo[];
  // Recursos recién compartidos
  recursos: Recurso[];
}

// Pantalla de inicio: resumen, próxima entrega y filas de tareas, grupos y recursos
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-inicio',
  // Componentes y utilidades que usa su plantilla
  imports: [DatePipe, RouterLink],
  // Archivo de la plantilla
  templateUrl: './inicio.html',
  // Archivo de los estilos
  styleUrl: './inicio.scss',
})
export class InicioPage implements OnInit {
  // Servicio de sesión
  private readonly auth = inject(AuthService);
  // Consulta del resumen
  private readonly resumenApi = inject(ResumenApi);
  // Consulta de tareas
  private readonly tareaApi = inject(TareaApi);
  // Consulta de grupos
  private readonly grupoApi = inject(GrupoApi);
  // Consulta de recursos
  private readonly recursoApi = inject(RecursoApi);

  // Datos de la pantalla, o nulo mientras cargan
  protected readonly panel = signal<Panel | null>(null);
  // Indica si se están cargando los datos
  protected readonly cargando = signal(true);
  // Mensaje de error, o nulo
  protected readonly error = signal<string | null>(null);

  // Primer nombre del usuario
  protected readonly nombreCorto = computed(() => (this.auth.usuario()?.nombre ?? '').split(' ')[0]);
  // Saludo según la hora del día
  protected readonly saludo = computed(() => {
    // Hora actual
    const hora = new Date().getHours();
    // Antes del mediodía
    if (hora < 12) {
      // Saludo de la mañana
      return 'Buenos días';
    }
    // Antes de las 7 de la noche
    if (hora < 19) {
      // Saludo de la tarde
      return 'Buenas tardes';
    }
    // Resto del día
    return 'Buenas noches';
  });

  // Texto de cada estado de tarea
  protected readonly textoEstado: Record<EstadoTarea, string> = { PENDIENTE: 'Pendiente', EN_PROGRESO: 'En progreso', HECHA: 'Hecha' };
  // Color de la etiqueta de cada estado
  protected readonly claseEstado: Record<EstadoTarea, string> = { PENDIENTE: 'or-chip', EN_PROGRESO: 'or-chip or-chip-ambar', HECHA: 'or-chip or-chip-azul' };
  // Texto de cada etiqueta de tarea
  protected readonly textoEtiqueta: Record<EtiquetaTarea, string> = {
    PROYECTO: 'Proyecto', LABORATORIO: 'Laboratorio', PRACTICA: 'Práctica', LECTURA: 'Lectura', QUIZ: 'Quiz', RESUMEN: 'Resumen', OTRO: 'Otro',
  };
  // Texto de cada tipo de recurso
  protected readonly textoTipo: Record<TipoRecurso, string> = { PDF: 'PDF', VIDEO: 'Video', DOCUMENTO: 'Documento', ENLACE: 'Enlace', QUIZ: 'Quiz' };

  // Carga los datos al abrir la pantalla
  ngOnInit(): void {
    // Reintenta la carga
    this.cargar();
  }

  // Pide todos los datos a la vez para mostrar la pantalla completa de una sola vez
  protected cargar(): void {
    // Muestra el estado de carga
    this.cargando.set(true);
    // Borra el error anterior
    this.error.set(null);
    // Ejecuta las cuatro consultas en paralelo
    forkJoin({
      // Estadísticas del usuario
      resumen: this.resumenApi.obtener(),
      // Tareas sin terminar, las seis primeras
      tareas: this.tareaApi.listar(undefined, 0, 20).pipe(map((pagina) => pagina.content.filter((tarea) => tarea.estado !== 'HECHA').slice(0, 6))),
      // Grupos del usuario
      grupos: this.grupoApi.listar(0, 6).pipe(map((pagina) => pagina.content)),
      // Recursos más recientes
      recursos: this.recursoApi.listar({ orden: 'RECIENTES' }, 0, 6).pipe(map((pagina) => pagina.content)),
    }).subscribe({
      // Si todo salió bien, guarda los datos
      next: (datos) => {
        // Guarda el panel
        this.panel.set(datos);
        // Termina la carga
        this.cargando.set(false);
      },
      // Si algo falló, muestra el motivo
      error: (error: unknown) => {
        // Guarda el mensaje
        this.error.set(mensajeDeError(error));
        // Termina la carga
        this.cargando.set(false);
      },
    });
  }
}
