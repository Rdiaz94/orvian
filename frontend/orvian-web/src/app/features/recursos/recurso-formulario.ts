// Utilidades de Angular
import { Component, OnInit, inject, input, output, signal } from '@angular/core';
// Formularios reactivos
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

// Servicio de recursos
import { RecursoApi } from '../../core/api/recurso.service';
// Conversión de errores en mensajes
import { mensajeDeError, problemaDe } from '../../core/errores';
// Tipos de datos
import { Curso, Grupo, Recurso, TipoRecurso } from '../../core/modelos';

// Diálogo para compartir o editar un recurso
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-recurso-formulario',
  // Módulos que usa su plantilla
  imports: [ReactiveFormsModule],
  // Archivo de la plantilla
  templateUrl: './recurso-formulario.html',
})
export class RecursoFormulario implements OnInit {
  // Recurso que se edita; es nulo cuando se comparte uno nuevo
  readonly recurso = input<Recurso | null>(null);
  // Cursos disponibles para elegir
  readonly cursos = input<Curso[]>([]);
  // Grupos con los que se puede compartir
  readonly grupos = input<Grupo[]>([]);
  // Se emite cuando se guardó el recurso
  readonly guardado = output<Recurso>();
  // Se emite cuando se cierra sin guardar
  readonly cancelado = output<void>();

  // Constructor de formularios con campos que nunca son nulos
  private readonly fb = inject(NonNullableFormBuilder);
  // Servicio de recursos
  private readonly api = inject(RecursoApi);

  // Indica si se está enviando el formulario
  protected readonly cargando = signal(false);
  // Mensaje de error general, o nulo
  protected readonly error = signal<string | null>(null);
  // Errores que la API detectó en cada campo
  protected readonly erroresServidor = signal<Record<string, string | undefined>>({});
  // Tipos que se pueden elegir
  protected readonly tipos: TipoRecurso[] = ['PDF', 'VIDEO', 'DOCUMENTO', 'ENLACE', 'QUIZ'];

  // Formulario con las mismas reglas que valida la API
  protected readonly formulario = this.fb.group({
    // El título es obligatorio y admite hasta 150 caracteres
    titulo: ['', [Validators.required, Validators.maxLength(150)]],
    // El tipo es obligatorio
    tipo: this.fb.control<TipoRecurso>('PDF'),
    // El enlace debe ser http o https y no tener espacios
    url: ['', [Validators.required, Validators.maxLength(500), Validators.pattern(/^https?:\/\/\S+$/i)]],
    // La descripción es opcional y admite hasta 500 caracteres
    descripcion: ['', [Validators.maxLength(500)]],
    // El curso es opcional
    cursoId: this.fb.control<number | null>(null),
    // El grupo es opcional; sin grupo el recurso es público
    grupoId: this.fb.control<number | null>(null),
  });

  // Carga los datos del recurso cuando se está editando
  ngOnInit(): void {
    // Recurso que se edita, si lo hay
    const recurso = this.recurso();
    // Rellena el formulario con sus datos
    this.formulario.reset({
      // Título actual
      titulo: recurso?.titulo ?? '',
      // Tipo actual
      tipo: recurso?.tipo ?? 'PDF',
      // Enlace actual
      url: recurso?.url ?? '',
      // Descripción actual
      descripcion: recurso?.descripcion ?? '',
      // Curso actual
      cursoId: recurso?.cursoId ?? null,
      // Grupo actual
      grupoId: recurso?.grupoId ?? null,
    });
    // La visibilidad no se puede cambiar al editar, porque haría público un contenido privado
    if (recurso) {
      // Bloquea el campo del grupo
      this.formulario.controls.grupoId.disable();
    }
  }

  // Indica si un campo debe mostrarse como inválido
  protected invalido(control: AbstractControl): boolean {
    // Se marca solo después de que la persona interactuó con el campo
    return control.invalid && (control.touched || control.dirty);
  }

  // Envía los datos a la API
  protected guardar(): void {
    // Si el formulario es inválido, se muestran los errores y no se envía
    if (this.formulario.invalid) {
      // Marca todos los campos para que muestren su error
      this.formulario.markAllAsTouched();
      // Termina
      return;
    }
    // Valores del formulario, incluido el grupo aunque esté bloqueado
    const valores = this.formulario.getRawValue();
    // Datos con la forma que espera la API
    const datos = {
      // Título sin espacios sobrantes
      titulo: valores.titulo.trim(),
      // Tipo elegido
      tipo: valores.tipo,
      // Enlace sin espacios sobrantes
      url: valores.url.trim(),
      // Un texto vacío se envía como nulo
      descripcion: valores.descripcion.trim() || null,
      // Curso elegido
      cursoId: valores.cursoId,
      // Grupo elegido
      grupoId: valores.grupoId,
    };
    // Recurso que se edita, si lo hay
    const existente = this.recurso();
    // Muestra el estado de carga
    this.cargando.set(true);
    // Borra los errores anteriores
    this.error.set(null);
    // Borra los errores por campo anteriores
    this.erroresServidor.set({});
    // Crea o edita según el caso
    (existente ? this.api.actualizar(existente.id, datos) : this.api.crear(datos)).subscribe({
      // Si salió bien, avisa a la pantalla
      next: (guardado) => this.guardado.emit(guardado),
      // Si falló, muestra el motivo
      error: (error: unknown) => {
        // Errores por campo que envía la API
        this.erroresServidor.set(problemaDe(error).errores ?? {});
        // Mensaje general
        this.error.set(mensajeDeError(error));
        // Termina el estado de carga
        this.cargando.set(false);
      },
    });
  }
}
