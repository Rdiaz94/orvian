// Tipo de flujo de datos
import { Observable } from 'rxjs';
// Utilidades de Angular
import { Component, OnInit, inject, input, output, signal } from '@angular/core';
// Formularios reactivos
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

// Servicios de tareas
import { TareaApi } from '../../core/api/tarea.service';
import { TareaGrupoApi } from '../../core/api/tarea-grupo.service';
// Conversión de errores en mensajes
import { mensajeDeError, problemaDe } from '../../core/errores';
// Conversión de fechas
import { isoALocal, localAIso } from '../../core/fechas';
// Tipos de datos
import { Curso, EtiquetaTarea, Integrante, Tarea, TareaRequest } from '../../core/modelos';

// Diálogo para crear o editar una tarea personal
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-tarea-formulario',
  // Módulos que usa su plantilla
  imports: [ReactiveFormsModule],
  // Archivo de la plantilla
  templateUrl: './tarea-formulario.html',
})
export class TareaFormulario implements OnInit {
  // Tarea que se edita; es nula cuando se crea una nueva
  readonly tarea = input<Tarea | null>(null);
  // Cursos disponibles para elegir
  readonly cursos = input<Curso[]>([]);
  // Grupo al que pertenece la tarea; es nulo cuando la tarea es personal
  readonly grupoId = input<number | null>(null);
  // Integrantes del grupo, para elegir a quién se asigna la tarea
  readonly integrantes = input<Integrante[]>([]);
  // Indica si quien edita puede asignar la tarea a otra persona; solo el líder
  readonly puedeAsignar = input(false);
  // Se emite con la tarea guardada
  readonly guardado = output<Tarea>();
  // Se emite cuando se cierra sin guardar
  readonly cancelado = output<void>();

  // Constructor de formularios con campos que nunca son nulos
  private readonly fb = inject(NonNullableFormBuilder);
  // Servicio de tareas personales
  private readonly api = inject(TareaApi);
  // Servicio de tareas de grupo
  private readonly grupoApi = inject(TareaGrupoApi);

  // Indica si se está enviando el formulario
  protected readonly cargando = signal(false);
  // Mensaje de error general, o nulo
  protected readonly error = signal<string | null>(null);
  // Errores que la API detectó en cada campo
  protected readonly erroresServidor = signal<Record<string, string | undefined>>({});
  // Etiquetas que se pueden elegir
  protected readonly etiquetas: EtiquetaTarea[] = ['PROYECTO', 'LABORATORIO', 'PRACTICA', 'LECTURA', 'QUIZ', 'RESUMEN', 'OTRO'];

  // Formulario con las mismas reglas que valida la API
  protected readonly formulario = this.fb.group({
    // El título es obligatorio y admite hasta 150 caracteres
    titulo: ['', [Validators.required, Validators.maxLength(150)]],
    // La descripción es opcional y admite hasta 1000 caracteres
    descripcion: ['', [Validators.maxLength(1000)]],
    // El curso es opcional
    cursoId: this.fb.control<number | null>(null),
    // La etiqueta por defecto es OTRO
    etiqueta: this.fb.control<EtiquetaTarea>('OTRO'),
    // La fecha límite es opcional
    fecha: [''],
    // Persona a la que se asigna, solo en tareas de grupo; si es nulo queda para quien la crea
    responsableId: this.fb.control<number | null>(null),
  });

  // Carga los datos de la tarea cuando se está editando
  ngOnInit(): void {
    // Tarea que se edita, si la hay
    const tarea = this.tarea();
    // Rellena el formulario con sus datos
    this.formulario.reset({
      // Título actual
      titulo: tarea?.titulo ?? '',
      // Descripción actual
      descripcion: tarea?.descripcion ?? '',
      // Curso actual
      cursoId: tarea?.cursoId ?? null,
      // Etiqueta actual
      etiqueta: tarea?.etiqueta ?? 'OTRO',
      // Fecha actual en el formato del campo
      fecha: isoALocal(tarea?.fechaVencimiento ?? null),
      // Responsable actual, solo en tareas de grupo
      responsableId: this.grupoId() !== null ? (tarea?.responsableId ?? null) : null,
    });
    // Quien no es líder solo puede crear tareas para sí mismo
    if (this.grupoId() !== null && !this.puedeAsignar()) {
      // Bloquea el campo de asignación
      this.formulario.controls.responsableId.disable();
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
    // Valores del formulario
    const valores = this.formulario.getRawValue();
    // Datos con la forma que espera la API
    const datos: TareaRequest = {
      // Título sin espacios sobrantes
      titulo: valores.titulo.trim(),
      // Un texto vacío se envía como nulo
      descripcion: valores.descripcion.trim() || null,
      // Curso elegido
      cursoId: valores.cursoId,
      // Etiqueta elegida
      etiqueta: valores.etiqueta,
      // La fecha se envía como instante UTC
      fechaVencimiento: localAIso(valores.fecha),
    };
    // Tarea que se edita, si la hay
    const existente = this.tarea();
    // Muestra el estado de carga
    this.cargando.set(true);
    // Borra los errores anteriores
    this.error.set(null);
    // Borra los errores por campo anteriores
    this.erroresServidor.set({});
    // Crea o edita según el caso
    this.enviar(existente, datos, valores.responsableId).subscribe({
      // Si salió bien, avisa a la pantalla
      next: (guardada) => this.guardado.emit(guardada),
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

  // Envía la tarea a la API personal o a la del grupo según corresponda
  private enviar(existente: Tarea | null, datos: TareaRequest, responsableId: number | null): Observable<Tarea> {
    // Grupo al que pertenece la tarea, si lo hay
    const grupoId = this.grupoId();
    // Las tareas personales usan la API personal
    if (grupoId === null) {
      // Crea o edita según el caso
      return existente ? this.api.actualizar(existente.id, datos) : this.api.crear(datos);
    }
    // Las tareas de grupo llevan además el responsable
    const datosGrupo = { ...datos, responsableId };
    // Crea o edita según el caso
    return existente ? this.grupoApi.actualizar(grupoId, existente.id, datosGrupo) : this.grupoApi.crear(grupoId, datosGrupo);
  }
}
