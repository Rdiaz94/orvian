// Tipo de los datos del usuario
import { Usuario } from '../core/modelos';

// Convierte un objeto en el formato base64 de URL que usan los tokens JWT
function aBase64Url(objeto: object): string {
  // Convierte a texto, luego a base64 y cambia los caracteres propios de la URL
  return btoa(JSON.stringify(objeto)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
}

// Crea un token con la forma de un JWT para las pruebas; su firma es falsa porque el frontend nunca la verifica
export function crearToken(expiraEnSegundos: number, rol = 'ESTUDIANTE', sub = '1'): string {
  // Momento de expiración en segundos desde 1970
  const exp = Math.floor(Date.now() / 1000) + expiraEnSegundos;
  // Cabecera, contenido y firma falsa separados por puntos
  return `${aBase64Url({ alg: 'HS256' })}.${aBase64Url({ sub, rol, exp })}.firma`;
}

// Usuario de ejemplo para las pruebas
export const USUARIO_PRUEBA: Usuario = {
  // Identificador
  id: 1,
  // Nombre
  nombre: 'Ana Pérez',
  // Correo
  correo: 'ana@correo.com',
  // Rol
  rol: 'ESTUDIANTE',
  // Carrera
  carrera: 'Ingeniería de Sistemas Computacionales',
  // Ciclo
  ciclo: 5,
};
