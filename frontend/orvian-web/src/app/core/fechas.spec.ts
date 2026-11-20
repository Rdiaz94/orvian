// Funciones que se prueban
import { estaVencida, isoALocal, localAIso } from './fechas';

// Pruebas de las conversiones de fechas
describe('fechas', () => {
  // Ida y vuelta entre el campo del formulario y la API
  it('convierte de hora local a ISO y de vuelta sin perder el minuto', () => {
    // Texto que produce un campo datetime-local
    const local = '2026-11-05T14:30';
    // Instante que se envía a la API
    const iso = localAIso(local);
    // Es un instante ISO 8601 en UTC
    expect(iso).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/);
    // Al volver se recupera el mismo texto local
    expect(isoALocal(iso)).toBe(local);
  });

  // Campos vacíos o inválidos
  it('tolera valores vacíos o inválidos', () => {
    // Un campo vacío no tiene fecha
    expect(localAIso('')).toBeNull();
    // Un texto sin sentido tampoco
    expect(localAIso('no es fecha')).toBeNull();
    // Sin instante el campo queda vacío
    expect(isoALocal(null)).toBe('');
    // Un instante inválido deja el campo vacío
    expect(isoALocal('basura')).toBe('');
  });

  // Vencimiento
  it('detecta si una fecha límite ya pasó', () => {
    // Momento de referencia
    const ahora = Date.parse('2026-11-05T12:00:00Z');
    // Una fecha anterior está vencida
    expect(estaVencida('2026-11-05T11:59:00Z', ahora)).toBe(true);
    // Una fecha posterior no lo está
    expect(estaVencida('2026-11-05T12:01:00Z', ahora)).toBe(false);
    // Sin fecha nunca vence
    expect(estaVencida(null, ahora)).toBe(false);
  });
});
