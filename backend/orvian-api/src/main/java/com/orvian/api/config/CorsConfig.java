// Paquete de las clases de configuración
package com.orvian.api.config;

// Intervalo de tiempo
import java.time.Duration;
// Lista de elementos
import java.util.List;

// Inyecta el valor de una propiedad del archivo de configuración
import org.springframework.beans.factory.annotation.Value;
// Registra componentes que Spring administra
import org.springframework.context.annotation.Bean;
// Anotación que marca la clase como fuente de configuración
import org.springframework.context.annotation.Configuration;
// Reglas CORS de la aplicación
import org.springframework.web.cors.CorsConfiguration;
// Contrato que entrega las reglas CORS según la ruta
import org.springframework.web.cors.CorsConfigurationSource;
// Asocia reglas CORS a patrones de rutas
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Indica que esta clase define componentes de configuración
@Configuration
// Define qué sitios web pueden llamar a la API desde un navegador
public class CorsConfig {

    // Spring Security busca un componente con este nombre para aplicar CORS
    @Bean
    // Crea las reglas a partir de los orígenes configurados
    public CorsConfigurationSource corsConfigurationSource(@Value("${orvian.cors.origenes-permitidos}") List<String> origenes) {
        // Un comodín permitiría que cualquier sitio llame a la API, así que se prohíbe
        if (origenes.contains("*")) {
            // Aborta el arranque con un mensaje claro
            throw new IllegalStateException("CORS no admite el comodín *: indica los orígenes exactos");
        }
        // Reglas CORS
        CorsConfiguration reglas = new CorsConfiguration();
        // Solo estos sitios pueden llamar a la API
        reglas.setAllowedOrigins(origenes);
        // Métodos HTTP permitidos
        reglas.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Cabeceras que el navegador puede enviar
        reglas.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // El token viaja en una cabecera, no en cookies, así que no se permiten credenciales
        reglas.setAllowCredentials(false);
        // El navegador puede recordar la verificación previa durante una hora
        reglas.setMaxAge(Duration.ofHours(1));
        // Fuente que asocia las reglas a las rutas
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        // Las reglas aplican solo a las rutas de la API
        fuente.registerCorsConfiguration("/api/**", reglas);
        // Devuelve la fuente de reglas
        return fuente;
    }
}
