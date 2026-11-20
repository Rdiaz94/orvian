// Paquete de las excepciones propias de la aplicación
package com.orvian.api.exception;

// Se lanza cuando se intenta registrar un correo que ya existe; es un tipo de conflicto
public class CorreoDuplicadoException extends ConflictoException {

    // Crea la excepción con un mensaje claro para el usuario
    public CorreoDuplicadoException(String correo) {
        // Envía el mensaje a la clase padre
        super("Ya existe un usuario registrado con el correo " + correo);
    }
}
