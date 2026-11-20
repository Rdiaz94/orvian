// Tipo de las guardas de rutas
import { CanActivateFn, Router } from '@angular/router';
// Utilidad de inyección
import { inject } from '@angular/core';

// Servicio de sesión
import { AuthService } from './auth.service';

// Permite entrar solo con una sesión vigente; si no, lleva al login
export const authGuard: CanActivateFn = () => {
  // Servicio de sesión
  const auth = inject(AuthService);
  // Navegador de pantallas
  const router = inject(Router);
  // Con sesión vigente deja pasar; si no, redirige al login
  return auth.sesionVigente() ? true : router.createUrlTree(['/login']);
};

// Impide que alguien con sesión vuelva al login o al registro
export const invitadoGuard: CanActivateFn = () => {
  // Servicio de sesión
  const auth = inject(AuthService);
  // Navegador de pantallas
  const router = inject(Router);
  // Con sesión vigente redirige al inicio; si no, deja pasar
  return auth.sesionVigente() ? router.createUrlTree(['/inicio']) : true;
};

// Permite entrar solo a los administradores; esta guarda solo mejora la experiencia, porque el servidor valida el rol en cada petición
export const adminGuard: CanActivateFn = () => {
  // Servicio de sesión
  const auth = inject(AuthService);
  // Navegador de pantallas
  const router = inject(Router);
  // Sin sesión vigente se va al login
  if (!auth.sesionVigente()) {
    // Redirige al login
    return router.createUrlTree(['/login']);
  }
  // Con sesión de administrador deja pasar; si no, vuelve al inicio
  return auth.esAdmin() ? true : router.createUrlTree(['/inicio']);
};
