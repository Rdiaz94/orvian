// Configuración de producción: se usa una ruta relativa porque el frontend y la API se publican bajo el mismo servidor
export const environment = {
  // La API está en la ruta orvian-api del mismo servidor, así que no hace falta CORS
  apiUrl: '/orvian-api',
  // Las direcciones llevan # (/#/tareas): al recargar la página el servidor nunca recibe la ruta interna, así que no hay 404 en ningún servidor
  usarHash: true,
};
