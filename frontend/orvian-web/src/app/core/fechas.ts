// Convierte el valor de un campo datetime-local, que está en hora local y sin zona, en un instante ISO 8601 en UTC
export function localAIso(valor: string): string | null {
  // Un campo vacío significa que no hay fecha
  if (!valor) {
    // Sin fecha
    return null;
  }
  // El navegador interpreta el texto como hora local
  const fecha = new Date(valor);
  // Un texto que no es una fecha válida se descarta
  return Number.isNaN(fecha.getTime()) ? null : fecha.toISOString();
}

// Convierte un instante ISO 8601 en el formato yyyy-MM-ddTHH:mm que espera un campo datetime-local, en hora local
export function isoALocal(iso: string | null): string {
  // Sin fecha el campo queda vacío
  if (!iso) {
    // Campo vacío
    return '';
  }
  // Lee el instante
  const fecha = new Date(iso);
  // Un texto que no es una fecha válida deja el campo vacío
  if (Number.isNaN(fecha.getTime())) {
    // Campo vacío
    return '';
  }
  // Agrega un cero a la izquierda cuando hace falta
  const dos = (numero: number): string => String(numero).padStart(2, '0');
  // Arma el texto con la fecha y la hora locales
  return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}T${dos(fecha.getHours())}:${dos(fecha.getMinutes())}`;
}

// Indica si una fecha límite ya pasó
export function estaVencida(iso: string | null, ahoraMs: number = Date.now()): boolean {
  // Sin fecha límite nunca vence
  if (!iso) {
    // No vence
    return false;
  }
  // Vence si el instante es anterior a ahora
  return new Date(iso).getTime() < ahoraMs;
}
