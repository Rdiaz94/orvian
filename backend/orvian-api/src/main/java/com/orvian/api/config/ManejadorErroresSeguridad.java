// Paquete de las clases de configuración
package com.orvian.api.config;

// Excepción de entrada y salida
import java.io.IOException;

// Convierte objetos Java en JSON
import com.fasterxml.jackson.databind.ObjectMapper;
// Petición HTTP del servlet
import jakarta.servlet.http.HttpServletRequest;
// Respuesta HTTP del servlet
import jakarta.servlet.http.HttpServletResponse;
// Nombres de las cabeceras HTTP
import org.springframework.http.HttpHeaders;
// Constantes de los códigos de estado HTTP
import org.springframework.http.HttpStatus;
// Tipos de contenido HTTP
import org.springframework.http.MediaType;
// Formato estándar (RFC 7807) para describir errores de una API
import org.springframework.http.ProblemDetail;
// Error por falta de permisos
import org.springframework.security.access.AccessDeniedException;
// Error por falta de autenticación
import org.springframework.security.core.AuthenticationException;
// Define qué hacer cuando falta autenticación
import org.springframework.security.web.AuthenticationEntryPoint;
// Define qué hacer cuando faltan permisos
import org.springframework.security.web.access.AccessDeniedHandler;
// Anotación que registra la clase como componente
import org.springframework.stereotype.Component;

// Registra la clase como componente de Spring
@Component
// Responde los errores de seguridad con el mismo formato JSON que el resto de la API
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    // Herramienta que convierte el error en JSON
    private final ObjectMapper mapeador;

    // Spring entrega el conversor por el constructor
    public ManejadorErroresSeguridad(ObjectMapper mapeador) {
        // Guarda el conversor
        this.mapeador = mapeador;
    }

    // Se ejecuta cuando falta el token o no es válido
    @Override
    // Responde 401 sin revelar detalles internos de la validación
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta, AuthenticationException error) throws IOException {
        // Indica al cliente que debe autenticarse con un token Bearer
        respuesta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        // Escribe la respuesta de error
        escribir(respuesta, HttpStatus.UNAUTHORIZED, "No autenticado", "Debes iniciar sesión con un token válido");
    }

    // Se ejecuta cuando el usuario está autenticado pero no tiene permiso
    @Override
    // Responde 403 con un mensaje genérico
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta, AccessDeniedException error) throws IOException {
        // Escribe la respuesta de error
        escribir(respuesta, HttpStatus.FORBIDDEN, "Acceso denegado", "No tienes permisos para realizar esta acción");
    }

    // Escribe un error en formato ProblemDetail dentro de la respuesta
    private void escribir(HttpServletResponse respuesta, HttpStatus estado, String titulo, String detalle) throws IOException {
        // Crea el error con su estado y detalle
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        // Asigna un título corto
        problema.setTitle(titulo);
        // Fija el código de estado HTTP
        respuesta.setStatus(estado.value());
        // Indica que el contenido es un error JSON estándar
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        // Usa UTF-8 para que se vean bien las tildes
        respuesta.setCharacterEncoding("UTF-8");
        // Escribe el JSON en el cuerpo de la respuesta
        mapeador.writeValue(respuesta.getOutputStream(), problema);
    }
}
