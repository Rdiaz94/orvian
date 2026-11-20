// Paquete de las excepciones propias de la aplicación
package com.orvian.api.exception;

// Se lanza cuando los datos son válidos en formato pero incumplen una regla de negocio
public class SolicitudInvalidaException extends RuntimeException {

    // Crea la excepción con un mensaje claro
    public SolicitudInvalidaException(String mensaje) {
        // Envía el mensaje a la clase padre
        super(mensaje);
    }
}
