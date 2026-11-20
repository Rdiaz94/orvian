// Indica si un enlace es seguro de abrir: solo se aceptan http y https, nunca javascript: ni data:
export function esUrlSegura(enlace: string): boolean {
  // Intenta interpretar el enlace
  try {
    // Protocolo del enlace
    const protocolo = new URL(enlace).protocol;
    // Solo los protocolos web son seguros
    return protocolo === 'http:' || protocolo === 'https:';
  } catch {
    // Un texto que no es un enlace válido no es seguro
    return false;
  }
}
