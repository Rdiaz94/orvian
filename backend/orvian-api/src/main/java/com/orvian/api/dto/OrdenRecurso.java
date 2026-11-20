// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Define el orden de una consulta
import org.springframework.data.domain.Sort;

// Órdenes permitidos para la biblioteca; el cliente elige uno, nunca un campo cualquiera
public enum OrdenRecurso {
    // Primero los más descargados
    POPULARES(Sort.by(Sort.Direction.DESC, "descargas").and(Sort.by(Sort.Direction.DESC, "id"))),
    // Primero los más recientes
    RECIENTES(Sort.by(Sort.Direction.DESC, "fechaCreacion").and(Sort.by(Sort.Direction.DESC, "id")));

    // Orden de la consulta que corresponde a esta opción
    private final Sort orden;

    // Asocia cada opción con su orden
    OrdenRecurso(Sort orden) {
        // Guarda el orden
        this.orden = orden;
    }

    // Devuelve el orden de la consulta
    public Sort orden() {
        // Retorna el orden
        return orden;
    }
}
