// Clase de error de las peticiones HTTP
import { HttpErrorResponse } from '@angular/common/http';

// Formato de error que devuelve la API
export interface ProblemaApi {
  // Título corto del error
  title?: string;
  // Explicación del error
  detail?: string;
  // Mensaje por cada campo inválido
  errores?: Record<string, string>;
}

// Extrae el cuerpo de error que envía la API, si lo hay
export function problemaDe(error: unknown): ProblemaApi {
  // Solo las respuestas HTTP con un cuerpo de tipo objeto traen el formato esperado
  if (error instanceof HttpErrorResponse && error.error !== null && typeof error.error === 'object') {
    // Devuelve el cuerpo con su tipo
    return error.error as ProblemaApi;
  }
  // Sin cuerpo reconocible se devuelve un objeto vacío
  return {};
}

// Convierte cualquier error en un mensaje comprensible para la persona usuaria
export function mensajeDeError(error: unknown): string {
  // Los errores HTTP tienen mensajes específicos
  if (error instanceof HttpErrorResponse) {
    // El estado 0 significa que la petición no llegó al servidor
    if (error.status === 0) {
      // Mensaje de conexión
      return 'No se pudo conectar con el servidor. Revisa que la API esté encendida.';
    }
    // El estado 429 es el límite de intentos
    if (error.status === 429) {
      // Mensaje de espera
      return 'Demasiados intentos. Espera unos minutos e inténtalo de nuevo.';
    }
    // La API explica el motivo en el detalle
    const detalle = problemaDe(error).detail;
    // Si hay detalle, se usa
    if (detalle) {
      // Devuelve el motivo
      return detalle;
    }
  }
  // Mensaje general para cualquier otro caso
  return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
}
