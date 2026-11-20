// Paquete de las excepciones propias de la aplicación
package com.orvian.api.exception;

// Se lanza cuando el usuario pertenece al recurso pero no tiene el permiso para esa acción
public class PermisoInsuficienteException extends RuntimeException {

    // Crea la excepción con un mensaje claro
    public PermisoInsuficienteException(String mensaje) {
        // Envía el mensaje a la clase padre
        super(mensaje);
    }
}
