// Utilidades de Angular
import { Component, inject, input, output, signal } from '@angular/core';
// Formularios reactivos
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

// Servicio de grupos
import { GrupoApi } from '../../core/api/grupo.service';
// Conversión de errores en mensajes
import { mensajeDeError, problemaDe } from '../../core/errores';
// Tipos de datos
import { Curso, Grupo } from '../../core/modelos';

// Diálogo para crear un grupo; quien lo crea queda como líder
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-grupo-formulario',
  // Módulos que usa su plantilla
  imports: [ReactiveFormsModule],
  // Archivo de la plantilla
  templateUrl: './grupo-formulario.html',
})
export class GrupoFormulario {
  // Cursos disponibles para elegir
  readonly cursos = input<Curso[]>([]);
  // Se emite con el grupo creado
  readonly creado = output<Grupo>();
  // Se emite cuando se cierra sin guardar
  readonly cancelado = output<void>();

  // Constructor de formularios con campos que nunca son nulos
  private readonly fb = inject(NonNullableFormBuilder);
  // Servicio de grupos
  private readonly api = inject(GrupoApi);

  // Indica si se está enviando el formulario
  protected readonly cargando = signal(false);
  // Mensaje de error general, o nulo
  protected readonly error = signal<string | null>(null);
  // Errores que la API detectó en cada campo
  protected readonly erroresServidor = signal<Record<string, string | undefined>>({});

  // Formulario con las mismas reglas que valida la API
  protected readonly formulario = this.fb.group({
    // El nombre es obligatorio y admite hasta 100 caracteres
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    // La descripción es opcional y admite hasta 500 caracteres
    descripcion: ['', [Validators.maxLength(500)]],
    // El curso es opcional
    cursoId: this.fb.control<number | null>(null),
  });

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
    // Muestra el estado de carga
    this.cargando.set(true);
    // Borra los errores anteriores
    this.error.set(null);
    // Borra los errores por campo anteriores
    this.erroresServidor.set({});
    // Crea el grupo
    this.api.crear({ nombre: valores.nombre.trim(), descripcion: valores.descripcion.trim() || null, cursoId: valores.cursoId }).subscribe({
      // Si salió bien, avisa a la pantalla
      next: (grupo) => this.creado.emit(grupo),
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
