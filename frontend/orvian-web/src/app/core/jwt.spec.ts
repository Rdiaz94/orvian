// Funciones que se prueban
import { decodificarToken, tokenVigente } from './jwt';
// Utilidad para crear tokens de prueba
import { crearToken } from '../testing/utilidades';

// Pruebas de la lectura de tokens
describe('jwt', () => {
  // Debe leer el sujeto, el rol y la expiración
  it('lee los datos de un token válido', () => {
    // Token de un administrador con identificador 7
    const datos = decodificarToken(crearToken(3600, 'ADMIN', '7'));
    // El sujeto es el esperado
    expect(datos?.sub).toBe('7');
    // El rol es el esperado
    expect(datos?.rol).toBe('ADMIN');
  });

  // Un texto cualquiera no es un token
  it('devuelve nulo si el texto no es un token', () => {
    // Texto sin la forma de un JWT
    expect(decodificarToken('no-es-un-token')).toBeNull();
    // Texto con tres partes pero sin contenido legible
    expect(decodificarToken('a.b.c')).toBeNull();
  });

  // Un token al que le faltan datos no sirve
  it('rechaza un token sin los datos esperados', () => {
    // Contenido sin expiración
    const incompleto = `x.${btoa(JSON.stringify({ sub: '1', rol: 'ESTUDIANTE' }))}.y`;
    // No se acepta
    expect(decodificarToken(incompleto)).toBeNull();
  });

  // La vigencia depende de la expiración
  it('distingue un token vigente de uno expirado', () => {
    // Un token que vence en una hora está vigente
    expect(tokenVigente(crearToken(3600))).toBe(true);
    // Un token que venció hace una hora no lo está
    expect(tokenVigente(crearToken(-3600))).toBe(false);
  });
});
