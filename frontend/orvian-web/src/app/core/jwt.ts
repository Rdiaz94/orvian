// Datos del token que el frontend necesita para mostrar la interfaz
export interface DatosToken {
  // Identificador del usuario
  sub: string;
  // Rol del usuario
  rol: string;
  // Momento de expiración en segundos desde 1970
  exp: number;
}

// Lee los datos de un token JWT; no verifica la firma, eso lo hace siempre el servidor
export function decodificarToken(token: string): DatosToken | null {
  // Intenta leer el contenido del token
  try {
    // La parte central del token contiene los datos
    const carga = token.split('.')[1];
    // Un token sin parte central no es válido
    if (!carga) {
      // No se puede leer
      return null;
    }
    // Convierte el formato base64 de URL en base64 normal
    const base64 = carga.replace(/-/g, '+').replace(/_/g, '/');
    // Convierte el base64 en texto, completando el relleno que falte
    const texto = atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '='));
    // Interpreta el texto como JSON
    const datos = JSON.parse(texto) as Partial<DatosToken>;
    // Comprueba que estén los datos que se necesitan
    if (typeof datos.sub !== 'string' || typeof datos.rol !== 'string' || typeof datos.exp !== 'number') {
      // El contenido no es el esperado
      return null;
    }
    // Devuelve solo lo necesario
    return { sub: datos.sub, rol: datos.rol, exp: datos.exp };
  } catch {
    // Cualquier error de lectura significa que el token no es válido
    return null;
  }
}

// Indica si el token es legible y todavía no expiró
export function tokenVigente(token: string, ahoraMs: number = Date.now()): boolean {
  // Lee los datos del token
  const datos = decodificarToken(token);
  // Es vigente si se pudo leer y su expiración está en el futuro
  return datos !== null && datos.exp * 1000 > ahoraMs;
}
