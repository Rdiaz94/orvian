// Tipos de peticiones HTTP y de errores
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
// Utilidad de inyección
import { inject } from '@angular/core';
// Navegación entre pantallas
import { Router } from '@angular/router';
// Operadores de flujos de datos
import { catchError, throwError } from 'rxjs';

// Dirección de la API según el entorno
import { environment } from '../../environments/environment';
// Servicio de sesión
import { AuthService } from './auth.service';

// Agrega el token a las peticiones dirigidas a nuestra API y a ninguna otra, para no filtrarlo a terceros
export const tokenInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  // Token de la sesión actual
  const token = inject(AuthService).token();
  // Solo se agrega si hay token y la petición va a la API de Orvian
  if (token !== null && peticion.url.startsWith(environment.apiUrl)) {
    // Envía una copia de la petición con la cabecera de autorización
    return siguiente(peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  }
  // En los demás casos la petición sigue sin cambios
  return siguiente(peticion);
};

// Si la API responde 401 con una sesión iniciada, el token venció: se cierra la sesión y se vuelve al login
export const sesionExpiradaInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  // Servicio de sesión
  const auth = inject(AuthService);
  // Navegador de pantallas
  const router = inject(Router);
  // Continúa con la petición y vigila los errores
  return siguiente(peticion).pipe(
    // Revisa cada error
    catchError((error: unknown) => {
      // El login y el registro responden 401 por credenciales incorrectas, no por sesión vencida
      const esPublica = peticion.url.endsWith('/api/auth/login') || peticion.url.endsWith('/api/auth/registro');
      // Una respuesta 401 en una ruta protegida con sesión iniciada significa que la sesión ya no sirve
      if (error instanceof HttpErrorResponse && error.status === 401 && !esPublica && auth.autenticado()) {
        // Cierra la sesión
        auth.cerrarSesion();
        // Lleva al login avisando el motivo
        void router.navigate(['/login'], { queryParams: { motivo: 'expirada' } });
      }
      // El error sigue su camino para que cada pantalla lo maneje
      return throwError(() => error);
    }),
  );
};
