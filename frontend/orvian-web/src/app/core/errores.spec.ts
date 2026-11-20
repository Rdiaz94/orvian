// Clase de error HTTP
import { HttpErrorResponse } from '@angular/common/http';

// Funciones que se prueban
import { mensajeDeError } from './errores';

// Pruebas de la conversión de errores en mensajes
describe('mensajeDeError', () => {
  // Sin conexión
  it('avisa cuando no se puede conectar con el servidor', () => {
    // El estado 0 es una petición que no llegó
    expect(mensajeDeError(new HttpErrorResponse({ status: 0 }))).toContain('No se pudo conectar');
  });

  // Límite de intentos
  it('explica el límite de intentos', () => {
    // El estado 429 es el exceso de intentos
    expect(mensajeDeError(new HttpErrorResponse({ status: 429 }))).toContain('Demasiados intentos');
  });

  // Mensaje enviado por la API
  it('usa el detalle que envía la API', () => {
    // Error con el formato de la API
    const error = new HttpErrorResponse({ status: 409, error: { title: 'Conflicto', detail: 'Ya existe un usuario con ese correo' } });
    // Se muestra el detalle
    expect(mensajeDeError(error)).toBe('Ya existe un usuario con ese correo');
  });

  // Cualquier otra cosa
  it('usa un mensaje general si no hay más información', () => {
    // Un error que no es HTTP
    expect(mensajeDeError(new Error('interno'))).toContain('error inesperado');
  });
});
