// Paquete raíz del backend de Orvian
package com.orvian.api;

// Clase que arranca la aplicación Spring
import org.springframework.boot.SpringApplication;
// Anotación que activa la configuración automática
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Marca esta clase como punto de entrada de Spring Boot
@SpringBootApplication
// Clase principal del backend
public class OrvianApiApplication {

    // Método que ejecuta la JVM al iniciar la aplicación
    public static void main(String[] args) {
        // Arranca el contexto de Spring y el servidor embebido
        SpringApplication.run(OrvianApiApplication.class, args);
    }
}
