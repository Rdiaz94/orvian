// Paquete de las excepciones propias de la aplicación
package com.orvian.api.exception;

// Mapa que conserva el orden de inserción
import java.util.LinkedHashMap;
// Interfaz de mapas
import java.util.Map;

// Cabeceras HTTP
import org.springframework.http.HttpHeaders;
// Código de estado HTTP genérico
import org.springframework.http.HttpStatusCode;
// Constantes de los códigos de estado HTTP
import org.springframework.http.HttpStatus;
// Formato estándar (RFC 7807) para describir errores de una API
import org.springframework.http.ProblemDetail;
// Respuesta HTTP con estado y cuerpo
import org.springframework.http.ResponseEntity;
// Excepción cuando el cuerpo de la petición no se puede leer
import org.springframework.http.converter.HttpMessageNotReadableException;
// Anotación que indica qué excepción atiende un método
import org.springframework.web.bind.annotation.ExceptionHandler;
// Anotación que aplica este manejo de errores a todos los controladores
import org.springframework.web.bind.annotation.RestControllerAdvice;
// Excepción que lanza Spring cuando falla la validación de un cuerpo
import org.springframework.web.bind.MethodArgumentNotValidException;
// Petición web en curso
import org.springframework.web.context.request.WebRequest;
// Base que ya convierte los errores estándar de Spring MVC en ProblemDetail
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Centraliza la conversión de excepciones en respuestas HTTP uniformes
@RestControllerAdvice
// Hereda el manejo de los errores estándar (405, 415, parámetros inválidos, etc.) y añade los propios
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Personaliza el error de validación para listar los campos inválidos
    @Override
    // Devuelve un 400 con el detalle de cada campo inválido
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException error, HttpHeaders cabeceras, HttpStatusCode estado, WebRequest peticion) {
        // Toma la respuesta que Spring ya prepara
        ProblemDetail respuesta = error.getBody();
        // Asigna un título corto
        respuesta.setTitle("Datos inválidos");
        // Asigna un detalle en español
        respuesta.setDetail("Hay datos inválidos en la petición");
        // Mapa donde se guarda un mensaje por campo
        Map<String, String> errores = new LinkedHashMap<>();
        // Recorre cada campo con error y conserva el primer mensaje de cada uno
        error.getBindingResult().getFieldErrors().forEach(campo -> errores.putIfAbsent(campo.getField(), campo.getDefaultMessage()));
        // Agrega los errores por campo a la respuesta
        respuesta.setProperty("errores", errores);
        // Devuelve la respuesta con el formato estándar
        return handleExceptionInternal(error, respuesta, cabeceras, estado, peticion);
    }

    // Personaliza el error cuando el JSON está mal formado o trae valores no permitidos
    @Override
    // Devuelve un 400 sin revelar detalles internos del lector de JSON
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException error, HttpHeaders cabeceras, HttpStatusCode estado, WebRequest peticion) {
        // Crea la respuesta con estado 400 y un mensaje genérico
        ProblemDetail respuesta = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido o contiene valores no permitidos");
        // Asigna un título corto
        respuesta.setTitle("Cuerpo ilegible");
        // Devuelve la respuesta con el formato estándar
        return handleExceptionInternal(error, respuesta, cabeceras, estado, peticion);
    }

    // Atiende cualquier conflicto con datos existentes: correo repetido, curso repetido, etc.
    @ExceptionHandler(ConflictoException.class)
    // Devuelve un 409 que indica un conflicto con los datos existentes
    public ProblemDetail conflicto(ConflictoException error) {
        // Construye y devuelve la respuesta
        return respuesta(HttpStatus.CONFLICT, "Conflicto con los datos existentes", error.getMessage());
    }

    // Atiende los recursos que no existen o que pertenecen a otro usuario
    @ExceptionHandler(NoEncontradoException.class)
    // Devuelve un 404 igual para "no existe" y "no es tuyo", para no revelar nada
    public ProblemDetail noEncontrado(NoEncontradoException error) {
        // Construye y devuelve la respuesta
        return respuesta(HttpStatus.NOT_FOUND, "No encontrado", error.getMessage());
    }

    // Atiende el exceso de intentos de inicio de sesión
    @ExceptionHandler(DemasiadosIntentosException.class)
    // Devuelve un 429 e indica en la cabecera Retry-After cuántos segundos esperar
    public ResponseEntity<ProblemDetail> demasiadosIntentos(DemasiadosIntentosException error) {
        // Construye la respuesta con un mensaje que no revela detalles de la cuenta
        ProblemDetail respuesta = respuesta(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos",
                "Superaste el número de intentos permitidos; inténtalo de nuevo más tarde");
        // Devuelve el estado 429 con la espera y el cuerpo de error
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, String.valueOf(error.getSegundosDeEspera())).body(respuesta);
    }

    // Atiende las acciones que el usuario no tiene permitidas dentro de un grupo
    @ExceptionHandler(PermisoInsuficienteException.class)
    // Devuelve un 403 con el motivo
    public ProblemDetail permisoInsuficiente(PermisoInsuficienteException error) {
        // Construye y devuelve la respuesta
        return respuesta(HttpStatus.FORBIDDEN, "Permiso insuficiente", error.getMessage());
    }

    // Atiende los datos que incumplen una regla de negocio
    @ExceptionHandler(SolicitudInvalidaException.class)
    // Devuelve un 400 con el motivo
    public ProblemDetail solicitudInvalida(SolicitudInvalidaException error) {
        // Construye y devuelve la respuesta
        return respuesta(HttpStatus.BAD_REQUEST, "Solicitud inválida", error.getMessage());
    }

    // Atiende los inicios de sesión fallidos
    @ExceptionHandler(CredencialesInvalidasException.class)
    // Devuelve un 401 con el mismo mensaje en todos los casos
    public ProblemDetail credencialesInvalidas(CredencialesInvalidasException error) {
        // Construye y devuelve la respuesta
        return respuesta(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", error.getMessage());
    }

    // Construye un error con el formato estándar en un solo lugar
    private ProblemDetail respuesta(HttpStatus estado, String titulo, String detalle) {
        // Crea la respuesta con su estado y detalle
        ProblemDetail respuesta = ProblemDetail.forStatusAndDetail(estado, detalle);
        // Asigna el título corto
        respuesta.setTitle(titulo);
        // Devuelve la respuesta
        return respuesta;
    }
}
