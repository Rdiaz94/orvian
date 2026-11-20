// Utilidades de Angular
import { Component, computed, input, output, signal } from '@angular/core';

// Tipos de datos
import { Recurso, TipoRecurso } from '../../core/modelos';

// Tarjeta de un recurso de la biblioteca
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-recurso-tarjeta',
  // Archivo de la plantilla
  templateUrl: './recurso-tarjeta.html',
  // Archivo de los estilos
  styleUrl: './recursos.scss',
})
export class RecursoTarjeta {
  // Recurso que se muestra
  readonly recurso = input.required<Recurso>();
  // Posición en la lista, para variar el color de la portada
  readonly indice = input(0);
  // Número de ranking cuando está en el Top 10
  readonly ranking = input<number | null>(null);
  // Se emite al pulsar Abrir
  readonly abrir = output<Recurso>();
  // Se emite al pulsar Editar
  readonly editar = output<Recurso>();
  // Se emite al confirmar el borrado
  readonly borrar = output<Recurso>();

  // Indica si se está pidiendo confirmación para borrar
  protected readonly confirmando = signal(false);
  // Clases de la portada según su posición
  protected readonly clasePortada = computed(() => `or-portada portada or-g${this.indice() % 6}`);
  // Texto de cada tipo de recurso
  protected readonly textoTipo: Record<TipoRecurso, string> = { PDF: 'PDF', VIDEO: 'Video', DOCUMENTO: 'Documento', ENLACE: 'Enlace', QUIZ: 'Quiz' };
  // Ícono de cada tipo de recurso
  protected readonly iconoTipo: Record<TipoRecurso, string> = {
    PDF: 'bi-file-earmark-pdf-fill', VIDEO: 'bi-play-circle-fill', DOCUMENTO: 'bi-file-earmark-text-fill', ENLACE: 'bi-link-45deg', QUIZ: 'bi-patch-question-fill',
  };
}
