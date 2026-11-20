// Paquete raíz del backend de Orvian
package com.orvian.api;

// Constructor para configurar el arranque de la aplicación
import org.springframework.boot.builder.SpringApplicationBuilder;
// Clase base para arrancar dentro de un Tomcat externo
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

// Permite que Tomcat externo inicie la aplicación al desplegar el WAR
public class ServletInitializer extends SpringBootServletInitializer {

    // Indica a Tomcat cuál es la clase principal de la aplicación
    @Override
    // Método que recibe el constructor de la aplicación
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        // Al desplegarse en un Tomcat externo se activa el perfil de producción, que toma sus claves de variables de entorno
        return application.profiles("prod").sources(OrvianApiApplication.class);
    }
}
