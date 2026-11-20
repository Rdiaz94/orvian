// Función que se prueba
import { esUrlSegura } from './enlaces';

// Pruebas de la validación de enlaces
describe('esUrlSegura', () => {
  // Enlaces web normales
  it('acepta enlaces http y https', () => {
    // Enlace seguro
    expect(esUrlSegura('https://ejemplo.com/guia.pdf')).toBe(true);
    // Enlace web sin cifrado, aceptado
    expect(esUrlSegura('http://ejemplo.com')).toBe(true);
  });

  // Enlaces peligrosos
  it('rechaza protocolos peligrosos y textos inválidos', () => {
    // Código ejecutable en el navegador
    expect(esUrlSegura('javascript:alert(1)')).toBe(false);
    // Contenido incrustado
    expect(esUrlSegura('data:text/html,<script>alert(1)</script>')).toBe(false);
    // Archivos locales
    expect(esUrlSegura('file:///C:/secreto.txt')).toBe(false);
    // Texto sin forma de enlace
    expect(esUrlSegura('no es un enlace')).toBe(false);
  });
});
