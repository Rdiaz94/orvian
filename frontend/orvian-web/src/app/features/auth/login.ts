// Utilidades de Angular
import { Component, inject, signal } from '@angular/core';
// Formularios reactivos
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
// Navegación entre pantallas
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

// Servicio de sesión
import { AuthService } from '../../core/auth.service';
// Conversión de errores en mensajes
import { mensajeDeError } from '../../core/errores';
// Componentes de marca compartidos
import { ArteAuth } from '../../shared/arte-auth';
import { LogoOrvian } from '../../shared/logo-orvian';

// Pantalla de inicio de sesión
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-login',
  // Componentes y módulos que usa su plantilla
  imports: [ReactiveFormsModule, RouterLink, ArteAuth, LogoOrvian],
  // Archivo de la plantilla
  templateUrl: './login.html',
  // Archivo de los estilos
  styleUrl: './auth.scss',
})
export class LoginPage {
  // Constructor de formularios con campos que nunca son nulos
  private readonly fb = inject(NonNullableFormBuilder);
  // Servicio de sesión
  private readonly auth = inject(AuthService);
  // Navegador de pantallas
  private readonly router = inject(Router);
  // Datos de la ruta actual
  private readonly ruta = inject(ActivatedRoute);

  // Indica si se está enviando el formulario
  protected readonly cargando = signal(false);
  // Mensaje de error para mostrar, o nulo
  protected readonly error = signal<string | null>(null);
  // Indica si la contraseña se muestra en texto claro
  protected readonly verClave = signal(false);
  // Indica si se llegó aquí porque la sesión venció
  protected readonly expirada = this.ruta.snapshot.queryParamMap.get('motivo') === 'expirada';

  // Formulario con sus validaciones
  protected readonly formulario = this.fb.group({
    // El correo es obligatorio y debe tener formato válido
    correo: ['', [Validators.required, Validators.email]],
    // La contraseña es obligatoria
    contrasena: ['', [Validators.required]],
  });

  // Indica si un campo debe mostrarse como inválido
  protected invalido(control: AbstractControl): boolean {
    // Se marca solo después de que la persona interactuó con el campo
    return control.invalid && (control.touched || control.dirty);
  }

  // Envía las credenciales a la API
  protected ingresar(): void {
    // Si el formulario es inválido, se muestran los errores y no se envía
    if (this.formulario.invalid) {
      // Marca todos los campos para que muestren su error
      this.formulario.markAllAsTouched();
      // Termina
      return;
    }
    // Muestra el estado de carga
    this.cargando.set(true);
    // Borra el error anterior
    this.error.set(null);
    // Llama a la API
    this.auth.login(this.formulario.getRawValue()).subscribe({
      // Si salió bien, entra a la aplicación
      next: () => void this.router.navigate(['/inicio']),
      // Si falló, muestra el motivo
      error: (error: unknown) => {
        // Guarda el mensaje
        this.error.set(mensajeDeError(error));
        // Termina el estado de carga
        this.cargando.set(false);
      },
    });
  }
}
