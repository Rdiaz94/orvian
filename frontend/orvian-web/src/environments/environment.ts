// Configuración al desarrollar: la API corre con mvn spring-boot:run en el puerto 8080
export const environment = {
  // Para probar contra el WAR en Tomcat cambia este valor por http://localhost:8080/orvian-api
  apiUrl: 'http://localhost:8080',
  // En desarrollo las direcciones son limpias (/tareas); el servidor de Angular ya sabe resolverlas
  usarHash: false,
};
