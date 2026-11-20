// Utilidades de Angular
import { Component, computed, inject, signal } from '@angular/core';
// Navegación entre pantallas
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

// Servicio de sesión
import { AuthService } from '../core/auth.service';

// Opción del menú de navegación
interface OpcionMenu {
  // Dirección de la pantalla
  ruta: string;
  // Texto que se muestra
  etiqueta: string;
  // Ícono de Bootstrap Icons
  icono: string;
  // Color del recuadro del ícono
  color: string;
}

// Opción que solo ven los administradores
const OPCION_ADMIN: OpcionMenu = { ruta: '/admin', etiqueta: 'Administración', icono: 'bi-shield-lock-fill', color: 'or-morado' };

// Opciones del menú, en el orden del diseño aprobado
const OPCIONES: OpcionMenu[] = [
  // Pantalla de inicio
  { ruta: '/inicio', etiqueta: 'Inicio', icono: 'bi-house-fill', color: 'or-ambar' },
  // Tablero de tareas
  { ruta: '/tareas', etiqueta: 'Mis tareas', icono: 'bi-check2-square', color: 'or-azul' },
  // Biblioteca de recursos
  { ruta: '/recursos', etiqueta: 'Recursos', icono: 'bi-journal-bookmark-fill', color: 'or-morado' },
  // Grupos de trabajo
  { ruta: '/grupos', etiqueta: 'Grupos', icono: 'bi-people-fill', color: 'or-azul' },
  // Perfil del usuario
  { ruta: '/perfil', etiqueta: 'Perfil', icono: 'bi-person-fill', color: 'or-ambar' },
];

// Estructura de las pantallas privadas: menú, barra superior y contenido
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-shell',
  // Componentes que usa su plantilla
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  // Archivo de la plantilla
  templateUrl: './shell.html',
  // Archivo de los estilos
  styleUrl: './shell.scss',
})
export class ShellPage {
  // Servicio de sesión, usado también por la plantilla
  protected readonly auth = inject(AuthService);
  // Navegador de pantallas
  private readonly router = inject(Router);
  // Opciones del menú; la de administración aparece solo para administradores
  protected readonly opciones = computed(() => (this.auth.esAdmin() ? [...OPCIONES, OPCION_ADMIN] : OPCIONES));
  // Texto escrito en el buscador
  protected readonly busqueda = signal('');
  // Iniciales del usuario para mostrar en su círculo
  protected readonly iniciales = computed(() => {
    // Nombre del usuario, o vacío si no hay sesión
    const nombre = this.auth.usuario()?.nombre ?? '';
    // Toma la primera letra de las dos primeras palabras
    return nombre.split(' ').filter(Boolean).slice(0, 2).map((palabra) => palabra[0]?.toUpperCase() ?? '').join('');
  });

  // Guarda lo que se escribe en el buscador
  protected alEscribir(evento: Event): void {
    // Campo de texto que originó el evento
    const campo = evento.target as HTMLInputElement;
    // Actualiza el texto
    this.busqueda.set(campo.value);
  }

  // Lleva a la biblioteca con el texto buscado
  protected buscar(evento: Event): void {
    // Evita que el formulario recargue la página
    evento.preventDefault();
    // Texto sin espacios sobrantes
    const texto = this.busqueda().trim();
    // Navega a recursos, con el texto como parámetro si lo hay
    void this.router.navigate(['/recursos'], { queryParams: texto ? { q: texto } : {} });
  }

  // Cierra la sesión y vuelve al login
  protected salir(): void {
    // Borra la sesión
    this.auth.cerrarSesion();
    // Navega al login
    void this.router.navigate(['/login']);
  }
}
