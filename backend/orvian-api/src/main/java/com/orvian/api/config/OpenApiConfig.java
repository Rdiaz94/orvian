// Paquete de las clases de configuración
package com.orvian.api.config;

// Modelo del documento OpenAPI
import io.swagger.v3.oas.models.OpenAPI;
// Componentes reutilizables del documento
import io.swagger.v3.oas.models.Components;
// Información general de la API
import io.swagger.v3.oas.models.info.Info;
// Exigencia de seguridad de la API
import io.swagger.v3.oas.models.security.SecurityRequirement;
// Esquema de seguridad de la API
import io.swagger.v3.oas.models.security.SecurityScheme;
// Utilidad de springdoc para ajustar qué se documenta
import org.springdoc.core.utils.SpringDocUtils;
// Registra componentes que Spring administra
import org.springframework.context.annotation.Bean;
// Anotación que marca la clase como fuente de configuración
import org.springframework.context.annotation.Configuration;
// Anotación que inyecta el usuario autenticado en un método
import org.springframework.security.core.annotation.AuthenticationPrincipal;

// Indica que esta clase define componentes de configuración
@Configuration
// Describe la API para la documentación interactiva
public class OpenApiConfig {

    // Nombre del esquema de seguridad que usan los endpoints protegidos
    private static final String ESQUEMA = "bearerAuth";

    // Bloque que se ejecuta una vez al cargar la clase
    static {
        // El usuario autenticado lo pone Spring desde el token, así que no es un parámetro que el cliente envíe
        SpringDocUtils.getConfig().addAnnotationsToIgnore(AuthenticationPrincipal.class);
    }

    // Registra la descripción general de la API
    @Bean
    // Crea el documento base con la información y el esquema de seguridad
    public OpenAPI orvianOpenApi() {
        // Construye el documento
        return new OpenAPI()
                // Información que se muestra en la cabecera de la documentación
                .info(new Info().title("Orvian API").version("0.0.1")
                        .description("API REST de Orvian: gestión académica y colaboración estudiantil. "
                                + "Inicia sesión en /api/auth/login y pega el token en el botón Authorize."))
                // Declara que los endpoints protegidos usan un token JWT en la cabecera Authorization
                .components(new Components().addSecuritySchemes(ESQUEMA,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                // Aplica ese esquema a toda la API
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}
