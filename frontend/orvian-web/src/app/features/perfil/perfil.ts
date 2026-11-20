// Utilidades de Angular
import { Component, OnInit, computed, inject, signal } from '@angular/core';
// Formularios reactivos
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
// Navegación entre pantallas
import { Router } from '@angular/router';

// Servicios de la API
import { PerfilApi } from '../../core/api/perfil.service';
import { ResumenApi } from '../../core/api/resumen.service';
// Servicio de sesión
import { AuthService } from '../../core/auth.service';
// Conversión de errores en mensajes
import { mensajeDeError, problemaDe } from '../../core/errores';
// Tipos de datos
import { ResumenAcademico } from '../../core/modelos';

// Comprueba que la contraseña nueva y su confirmación sean iguales
function coincidenContrasenas(grupo: AbstractControl): ValidationErrors | null {
  // Contraseña nueva
  const nueva = grupo.get('nueva')?.value as string | undefined;
  // Confirmación escrita
  const confirmacion = grupo.get('confirmacion')?.value as string | undefined;
  // Si son iguales no hay error
  return nueva === confirmacion ? null : { noCoinciden: true };
}

// Pantalla de perfil: datos personales, estadísticas y cambio de contraseña
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-perfil',
  // Módulos que usa su plantilla
  imports: [ReactiveFormsModule],
  // Archivo de la plantilla
  templateUrl: './perfil.html',
})
export class PerfilPage implements OnInit {
  // Constructor de formularios con campos que nunca son nulos
  private readonly fb = inject(NonNullableFormBuilder);
  // Servicio de sesión, usado también por la plantilla
  protected readonly auth = inject(AuthService);
  // Servicio del perfil
  private readonly api = inject(PerfilApi);
  // Servicio del resumen
  private readonly resumenApi = inject(ResumenApi);
  // Navegador de pantallas
  private readonly router = inject(Router);

  // Estadísticas del usuario, o nulo mientras cargan
  protected readonly resumen = signal<ResumenAcademico | null>(null);
  // Indica si se están guardando los datos personales
  protected readonly guardandoDatos = signal(false);
  // Indica si se está cambiando la contraseña
  protected readonly guardandoClave = signal(false);
  // Mensaje de éxito de los datos personales, o nulo
  protected readonly mensajeDatos = signal<string | null>(null);
  // Mensaje de éxito de la contraseña, o nulo
  protected readonly mensajeClave = signal<string | null>(null);
  // Error de los datos personales, o nulo
  protected readonly errorDatos = signal<string | null>(null);
  // Error de la contraseña, o nulo
  protected readonly errorClave = signal<string | null>(null);
  // Errores que la API detectó en cada campo de los datos personales
  protected readonly erroresServidor = signal<Record<string, string | undefined>>({});

  // Iniciales del usuario para mostrar en su círculo
  protected readonly iniciales = computed(() => {
    // Nombre del usuario, o vacío si no hay sesión
    const nombre = this.auth.usuario()?.nombre ?? '';
    // Toma la primera letra de las dos primeras palabras
    return nombre.split(' ').filter(Boolean).slice(0, 2).map((palabra) => palabra[0]?.toUpperCase() ?? '').join('');
  });

  // Formulario de datos personales con las mismas reglas que valida la API
  protected readonly datos = this.fb.group({
    // El nombre es obligatorio y admite hasta 100 caracteres
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    // La carrera es opcional
    carrera: ['', [Validators.maxLength(100)]],
    // El ciclo es opcional y va de 1 a 12
    ciclo: this.fb.control<number | null>(null, [Validators.min(1), Validators.max(12)]),
  });

  // Formulario de cambio de contraseña
  protected readonly clave = this.fb.group(
    {
      // La contraseña actual es obligatoria
      actual: ['', [Validators.required]],
      // La contraseña nueva debe tener entre 8 y 72 caracteres
      nueva: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      // La confirmación es obligatoria
      confirmacion: ['', [Validators.required]],
    },
    { validators: [coincidenContrasenas] },
  );

  // Carga las estadísticas y rellena el formulario con los datos de la sesión
  ngOnInit(): void {
    // Usuario de la sesión
    const usuario = this.auth.usuario();
    // Rellena el formulario de datos personales
    this.datos.reset({ nombre: usuario?.nombre ?? '', carrera: usuario?.carrera ?? '', ciclo: usuario?.ciclo ?? null });
    // Pide las estadísticas
    this.resumenApi.obtener().subscribe({ next: (resumen) => this.resumen.set(resumen), error: (error: unknown) => this.errorDatos.set(mensajeDeError(error)) });
  }

  // Indica si un campo debe mostrarse como inválido
  protected invalido(control: AbstractControl): boolean {
    // Se marca solo después de que la persona interactuó con el campo
    return control.invalid && (control.touched || control.dirty);
  }

  // Guarda los datos personales
  protected guardarDatos(): void {
    // Si el formulario es inválido, se muestran los errores y no se envía
    if (this.datos.invalid) {
      // Marca todos los campos para que muestren su error
      this.datos.markAllAsTouched();
      // Termina
      return;
    }
    // Valores del formulario
    const valores = this.datos.getRawValue();
    // Muestra el estado de carga
    this.guardandoDatos.set(true);
    // Borra los mensajes anteriores
    this.mensajeDatos.set(null);
    this.errorDatos.set(null);
    this.erroresServidor.set({});
    // Envía los datos a la API
    this.api.actualizar({ nombre: valores.nombre.trim(), carrera: valores.carrera.trim() || null, ciclo: valores.ciclo }).subscribe({
      // Si salió bien, actualiza la sesión para que el menú muestre el nombre nuevo
      next: (usuario) => {
        // Actualiza el usuario de la sesión
        this.auth.actualizarUsuario(usuario);
        // Avisa del éxito
        this.mensajeDatos.set('Datos actualizados.');
        // Termina la carga
        this.guardandoDatos.set(false);
      },
      // Si falló, muestra el motivo
      error: (error: unknown) => {
        // Errores por campo que envía la API
        this.erroresServidor.set(problemaDe(error).errores ?? {});
        // Mensaje general
        this.errorDatos.set(mensajeDeError(error));
        // Termina la carga
        this.guardandoDatos.set(false);
      },
    });
  }

  // Cambia la contraseña
  protected cambiarClave(): void {
    // Si el formulario es inválido, se muestran los errores y no se envía
    if (this.clave.invalid) {
      // Marca todos los campos para que muestren su error
      this.clave.markAllAsTouched();
      // Termina
      return;
    }
    // Valores del formulario
    const valores = this.clave.getRawValue();
    // Muestra el estado de carga
    this.guardandoClave.set(true);
    // Borra los mensajes anteriores
    this.mensajeClave.set(null);
    this.errorClave.set(null);
    // Envía solo la actual y la nueva; la confirmación nunca sale del navegador
    this.api.cambiarContrasena({ contrasenaActual: valores.actual, contrasenaNueva: valores.nueva }).subscribe({
      // Si salió bien, limpia el formulario
      next: () => {
        // Borra lo escrito para que las contraseñas no queden en pantalla
        this.clave.reset();
        // Avisa del éxito
        this.mensajeClave.set('Contraseña actualizada. La próxima vez inicia sesión con la nueva.');
        // Termina la carga
        this.guardandoClave.set(false);
      },
      // Si falló, muestra el motivo
      error: (error: unknown) => {
        // Mensaje general
        this.errorClave.set(mensajeDeError(error));
        // Termina la carga
        this.guardandoClave.set(false);
      },
    });
  }

  // Cierra la sesión y vuelve al login
  protected salir(): void {
    // Borra la sesión
    this.auth.cerrarSesion();
    // Navega al login
    void this.router.navigate(['/login']);
  }
}
