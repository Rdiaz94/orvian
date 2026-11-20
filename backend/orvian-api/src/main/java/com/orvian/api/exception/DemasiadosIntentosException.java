// Paquete de las excepciones propias de la aplicación
package com.orvian.api.exception;

// Se lanza cuando se superan los intentos fallidos de inicio de sesión permitidos
public class DemasiadosIntentosException extends RuntimeException {

    // Segundos que debe esperar el cliente antes de volver a intentarlo
    private final long segundosDeEspera;

    // Crea la excepción indicando cuánto falta para poder reintentar
    public DemasiadosIntentosException(long segundosDeEspera) {
        // El mensaje es genérico y no revela detalles de la cuenta
        super("Demasiados intentos fallidos");
        // Guarda la espera
        this.segundosDeEspera = segundosDeEspera;
    }

    // Devuelve los segundos de espera
    public long getSegundosDeEspera() {
        // Retorna la espera
        return segundosDeEspera;
    }
}
