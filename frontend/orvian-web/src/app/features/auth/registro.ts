// Utilidades de Angular
import { Component, inject, signal } from '@angular/core';
// Formularios reactivos
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
// Navegación entre pantallas
import { Router, RouterLink } from '@angular/router';
// Operadores de flujos de datos
import { switchMap } from 'rxjs';

// Servicio de sesión
import { AuthService } from '../../core/auth.service';
// Conversión de errores en mensajes
import { mensajeDeError, problemaDe } from '../../core/errores';
// Componentes de marca compartidos
import { ArteAuth } from '../../shared/arte-auth';
import { LogoOrvian } from '../../shared/logo-orvian';

// Pantalla de registro de usuarios nuevos
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-registro',
  // Componentes y módulos que usa su plantilla
  imports: [ReactiveFormsModule, RouterLink, ArteAuth, LogoOrvian],
  // Archivo de la plantilla
  templateUrl: './registro.html',
  // Archivo de los estilos, compartidos con el login
  styleUrl: './auth.scss',
})
export class RegistroPage {
  // Constructor de formularios con campos que nunca son nulos
  private readonly fb = inject(NonNullableFormBuilder);
  // Servicio de sesión
  private readonly auth = inject(AuthService);
  // Navegador de pantallas
  private readonly router = inject(Router);

  // Indica si la contraseña se muestra en texto claro
  protected readonly verClave = signal(false);
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
    // El correo es obligatorio y debe tener formato válido
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    // La contraseña debe tener entre 8 y 72 caracteres
    contrasena: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    // La carrera es opcional
    carrera: ['', [Validators.maxLength(100)]],
    // El ciclo es opcional y va de 1 a 12
    ciclo: this.fb.control<number | null>(null, [Validators.min(1), Validators.max(12)]),
  });

  // Indica si un campo debe mostrarse como inválido
  protected invalido(control: AbstractControl): boolean {
    // Se marca solo después de que la persona interactuó con el campo
    return control.invalid && (control.touched || control.dirty);
  }

  // Registra al usuario y, si sale bien, inicia su sesión
  protected registrar(): void {
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
    // Registra y, si sale bien, inicia sesión con las mismas credenciales
    this.auth
      .registro({
        // Nombre sin espacios sobrantes
        nombre: valores.nombre.trim(),
        // Correo sin espacios sobrantes
        correo: valores.correo.trim(),
        // La contraseña se envía tal cual, sin recortar
        contrasena: valores.contrasena,
        // Un texto vacío se envía como nulo
        carrera: valores.carrera.trim() || null,
        // El ciclo ya es un número o nulo
        ciclo: valores.ciclo,
      })
      .pipe(switchMap(() => this.auth.login({ correo: valores.correo.trim(), contrasena: valores.contrasena })))
      .subscribe({
        // Si todo salió bien, entra a la aplicación
        next: () => void this.router.navigate(['/inicio']),
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
