// Utilidades de Angular
import { Component, OnInit, inject, signal } from '@angular/core';
// Formularios reactivos
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

// Servicios de la API
import { AdminApi } from '../../core/api/admin.service';
import { CursoApi } from '../../core/api/curso.service';
// Conversión de errores en mensajes
import { mensajeDeError } from '../../core/errores';
// Tipos de datos
import { Curso, Usuario } from '../../core/modelos';

// Cantidad de usuarios por página
const TAMANO_PAGINA = 20;

// Pestañas de la pantalla
type Pestana = 'usuarios' | 'cursos';

// Pantalla de administración: usuarios y cursos
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-admin',
  // Módulos que usa su plantilla
  imports: [ReactiveFormsModule],
  // Archivo de la plantilla
  templateUrl: './admin.html',
})
export class AdminPage implements OnInit {
  // Constructor de formularios con campos que nunca son nulos
  private readonly fb = inject(NonNullableFormBuilder);
  // Servicio de administración
  private readonly api = inject(AdminApi);
  // Servicio de cursos
  private readonly cursoApi = inject(CursoApi);

  // Pestaña que se muestra
  protected readonly pestana = signal<Pestana>('usuarios');
  // Usuarios de la página actual
  protected readonly usuarios = signal<Usuario[]>([]);
  // Número de la página actual, empezando en cero
  protected readonly pagina = signal(0);
  // Total de páginas
  protected readonly totalPaginas = signal(0);
  // Total de usuarios
  protected readonly totalUsuarios = signal(0);
  // Indica si se están cargando los usuarios
  protected readonly cargandoUsuarios = signal(true);
  // Cursos existentes
  protected readonly cursos = signal<Curso[]>([]);
  // Indica si se está creando un curso
  protected readonly creando = signal(false);
  // Mensaje de error, o nulo
  protected readonly error = signal<string | null>(null);
  // Mensaje de éxito, o nulo
  protected readonly mensaje = signal<string | null>(null);

  // Formulario para crear un curso
  protected readonly formulario = this.fb.group({
    // El nombre es obligatorio y admite hasta 100 caracteres
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
  });

  // Carga los datos al abrir la pantalla
  ngOnInit(): void {
    // Primera página de usuarios
    this.cargarUsuarios(0);
    // Lista de cursos
    this.cargarCursos();
  }

  // Cambia la pestaña visible
  protected elegir(pestana: Pestana): void {
    // Guarda la pestaña
    this.pestana.set(pestana);
  }

  // Carga una página de usuarios
  protected cargarUsuarios(pagina: number): void {
    // Muestra el estado de carga
    this.cargandoUsuarios.set(true);
    // Consulta la API
    this.api.listarUsuarios(pagina, TAMANO_PAGINA).subscribe({
      // Si salió bien, guarda los datos
      next: (resultado) => {
        // Usuarios de la página
        this.usuarios.set(resultado.content);
        // Página actual
        this.pagina.set(resultado.page.number);
        // Total de páginas
        this.totalPaginas.set(resultado.page.totalPages);
        // Total de usuarios
        this.totalUsuarios.set(resultado.page.totalElements);
        // Termina la carga
        this.cargandoUsuarios.set(false);
      },
      // Si falló, muestra el motivo
      error: (error: unknown) => {
        // Guarda el mensaje
        this.error.set(mensajeDeError(error));
        // Termina la carga
        this.cargandoUsuarios.set(false);
      },
    });
  }

  // Carga la lista de cursos
  protected cargarCursos(): void {
    // Consulta la API
    this.cursoApi.listar().subscribe({ next: (cursos) => this.cursos.set(cursos), error: (error: unknown) => this.error.set(mensajeDeError(error)) });
  }

  // Crea un curso nuevo
  protected crearCurso(): void {
    // Si el formulario es inválido, se muestra el error y no se envía
    if (this.formulario.invalid) {
      // Marca el campo para que muestre su error
      this.formulario.markAllAsTouched();
      // Termina
      return;
    }
    // Muestra el estado de carga
    this.creando.set(true);
    // Borra los mensajes anteriores
    this.error.set(null);
    this.mensaje.set(null);
    // Envía el nombre a la API
    this.api.crearCurso(this.formulario.getRawValue().nombre.trim()).subscribe({
      // Si salió bien, limpia el formulario y recarga la lista
      next: (curso) => {
        // Borra lo escrito
        this.formulario.reset();
        // Avisa del éxito
        this.mensaje.set(`Curso "${curso.nombre}" creado.`);
        // Termina la carga
        this.creando.set(false);
        // Recarga los cursos
        this.cargarCursos();
      },
      // Si falló, muestra el motivo, por ejemplo un curso repetido
      error: (error: unknown) => {
        // Guarda el mensaje
        this.error.set(mensajeDeError(error));
        // Termina la carga
        this.creando.set(false);
      },
    });
  }
}
