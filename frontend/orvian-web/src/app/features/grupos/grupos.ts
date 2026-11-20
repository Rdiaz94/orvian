// Utilidades de Angular
import { Component, OnInit, inject, signal } from '@angular/core';
// Navegación entre pantallas
import { Router, RouterLink } from '@angular/router';

// Servicios de la API
import { CursoApi } from '../../core/api/curso.service';
import { GrupoApi } from '../../core/api/grupo.service';
// Conversión de errores en mensajes
import { mensajeDeError } from '../../core/errores';
// Tipos de datos
import { Curso, Grupo } from '../../core/modelos';
// Diálogo para crear un grupo
import { GrupoFormulario } from './grupo-formulario';

// Cantidad de grupos por página
const TAMANO_PAGINA = 12;

// Lista de los grupos del usuario
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-grupos',
  // Componentes que usa su plantilla
  imports: [RouterLink, GrupoFormulario],
  // Archivo de la plantilla
  templateUrl: './grupos.html',
})
export class GruposPage implements OnInit {
  // Servicio de grupos
  private readonly api = inject(GrupoApi);
  // Servicio de cursos
  private readonly cursoApi = inject(CursoApi);
  // Navegador de pantallas
  private readonly router = inject(Router);

  // Grupos cargados
  protected readonly grupos = signal<Grupo[]>([]);
  // Total de grupos del usuario
  protected readonly total = signal(0);
  // Última página cargada
  protected readonly pagina = signal(0);
  // Indica si se están cargando datos
  protected readonly cargando = signal(true);
  // Mensaje de error, o nulo
  protected readonly error = signal<string | null>(null);
  // Cursos para el formulario
  protected readonly cursos = signal<Curso[]>([]);
  // Indica si el diálogo está abierto
  protected readonly dialogoAbierto = signal(false);

  // Carga los datos al abrir la pantalla
  ngOnInit(): void {
    // Carga la primera página de grupos
    this.cargar(0);
    // Carga los cursos para el formulario
    this.cursoApi.listar().subscribe({ next: (cursos) => this.cursos.set(cursos), error: (error: unknown) => this.error.set(mensajeDeError(error)) });
  }

  // Indica si quedan grupos sin cargar
  protected hayMas(): boolean {
    // Hay más si lo cargado es menor que el total
    return this.grupos().length < this.total();
  }

  // Carga una página de grupos; la primera reemplaza el contenido y las demás se agregan
  protected cargar(pagina: number): void {
    // Muestra el estado de carga
    this.cargando.set(true);
    // Consulta la API
    this.api.listar(pagina, TAMANO_PAGINA).subscribe({
      // Si salió bien, guarda los datos
      next: (resultado) => {
        // La primera página reemplaza; las demás se agregan
        this.grupos.update((actuales) => (pagina === 0 ? resultado.content : [...actuales, ...resultado.content]));
        // Total de grupos
        this.total.set(resultado.page.totalElements);
        // Página cargada
        this.pagina.set(pagina);
        // Termina la carga
        this.cargando.set(false);
      },
      // Si falló, muestra el motivo
      error: (error: unknown) => {
        // Guarda el mensaje
        this.error.set(mensajeDeError(error));
        // Termina la carga
        this.cargando.set(false);
      },
    });
  }

  // Cierra el diálogo y lleva al grupo recién creado
  protected alCrear(grupo: Grupo): void {
    // Cierra el diálogo
    this.dialogoAbierto.set(false);
    // Navega al detalle del grupo
    void this.router.navigate(['/grupos', grupo.id]);
  }
}
