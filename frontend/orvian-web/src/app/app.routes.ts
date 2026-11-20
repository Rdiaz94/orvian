// Tipo de las rutas
import { Routes } from '@angular/router';

// Guardas de acceso
import { adminGuard, authGuard, invitadoGuard } from './core/guardas';
// Estructura con barra lateral
import { ShellPage } from './layout/shell';

// Rutas de la aplicación; las pantallas se cargan solo cuando se visitan
export const routes: Routes = [
  // Pantalla de inicio de sesión, solo para quien no tiene sesión
  { path: 'login', canActivate: [invitadoGuard], loadComponent: () => import('./features/auth/login').then((m) => m.LoginPage) },
  // Pantalla de registro, solo para quien no tiene sesión
  { path: 'registro', canActivate: [invitadoGuard], loadComponent: () => import('./features/auth/registro').then((m) => m.RegistroPage) },
  // Zona privada con barra lateral
  {
    // Ruta raíz
    path: '',
    // Estructura común de las pantallas privadas
    component: ShellPage,
    // Solo entra quien tiene sesión vigente
    canActivate: [authGuard],
    // Pantallas internas
    children: [
      // La raíz lleva al inicio
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      // Pantalla de inicio
      { path: 'inicio', loadComponent: () => import('./features/inicio/inicio').then((m) => m.InicioPage) },
      // Tablero de tareas
      { path: 'tareas', loadComponent: () => import('./features/tareas/tareas').then((m) => m.TareasPage) },
      // Biblioteca de recursos
      { path: 'recursos', loadComponent: () => import('./features/recursos/recursos').then((m) => m.RecursosPage) },
      // Lista de grupos
      { path: 'grupos', loadComponent: () => import('./features/grupos/grupos').then((m) => m.GruposPage) },
      // Detalle de un grupo
      { path: 'grupos/:id', loadComponent: () => import('./features/grupos/grupo-detalle').then((m) => m.GrupoDetallePage) },
      // Administración, solo para administradores
      { path: 'admin', canActivate: [adminGuard], loadComponent: () => import('./features/admin/admin').then((m) => m.AdminPage) },
      // Perfil del usuario
      { path: 'perfil', loadComponent: () => import('./features/perfil/perfil').then((m) => m.PerfilPage) },
    ],
  },
  // Cualquier otra dirección lleva a la raíz
  { path: '**', redirectTo: '' },
];
