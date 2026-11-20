// Paquete de las excepciones propias de la aplicación
package com.orvian.api.exception;

// Se lanza cuando un recurso no existe o no pertenece al usuario que lo pide
public class NoEncontradoException extends RuntimeException {

    // Crea la excepción con un mensaje claro
    public NoEncontradoException(String mensaje) {
        // Envía el mensaje a la clase padre
        super(mensaje);
    }
}
