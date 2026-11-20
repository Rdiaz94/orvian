// Utilidad para datos de idioma
import { registerLocaleData } from '@angular/common';
// Datos del idioma español
import localeEs from '@angular/common/locales/es';
// Cliente HTTP y sus opciones
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
// Configuración base de Angular
import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
// Enrutamiento
import { provideRouter, withComponentInputBinding, withHashLocation } from '@angular/router';

// Dirección de la API y modo de direcciones según el entorno
import { environment } from '../environments/environment';
// Rutas de la aplicación
import { routes } from './app.routes';
// Interceptores de la sesión
import { sesionExpiradaInterceptor, tokenInterceptor } from './core/interceptores';

// Carga los formatos de fecha en español
registerLocaleData(localeEs);

// Configuración global de la aplicación
export const appConfig: ApplicationConfig = {
  // Servicios disponibles en toda la aplicación
  providers: [
    // Registra los errores no controlados del navegador
    provideBrowserGlobalErrorListeners(),
    // Habilita las rutas y que los datos de la ruta lleguen como entradas del componente; en producción usa direcciones con # para no depender de la configuración del servidor
    provideRouter(routes, withComponentInputBinding(), ...(environment.usarHash ? [withHashLocation()] : [])),
    // Habilita el cliente HTTP con los interceptores de sesión
    provideHttpClient(withFetch(), withInterceptors([tokenInterceptor, sesionExpiradaInterceptor])),
    // Idioma de la aplicación para fechas y números
    { provide: LOCALE_ID, useValue: 'es' },
  ],
};
