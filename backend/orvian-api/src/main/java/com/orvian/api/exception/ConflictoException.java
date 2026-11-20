// Paquete de las excepciones propias de la aplicación
package com.orvian.api.exception;

// Se lanza cuando una operación choca con datos que ya existen
public class ConflictoException extends RuntimeException {

    // Crea la excepción con un mensaje claro para el usuario
    public ConflictoException(String mensaje) {
        // Envía el mensaje a la clase padre
        super(mensaje);
    }
}
