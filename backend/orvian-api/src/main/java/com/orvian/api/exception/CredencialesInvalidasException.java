// Paquete de las excepciones propias de la aplicación
package com.orvian.api.exception;

// Se lanza cuando el correo o la contraseña no son correctos
public class CredencialesInvalidasException extends RuntimeException {

    // Crea la excepción con un mensaje genérico
    public CredencialesInvalidasException() {
        // El mensaje no revela si falló el correo o la contraseña
        super("Correo o contraseña incorrectos");
    }
}
