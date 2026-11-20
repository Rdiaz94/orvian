// Paquete de las utilidades compartidas
package com.orvian.api.util;

// Crea una solicitud de página
import org.springframework.data.domain.PageRequest;
// Define el orden de una consulta
import org.springframework.data.domain.Sort;

// Reglas comunes de paginación para todos los listados
public final class Paginacion {

    // Máximo de elementos por página para que nadie pida una tabla completa de golpe
    private static final int TAMANO_MAXIMO = 50;

    // Impide crear instancias: la clase solo ofrece métodos estáticos
    private Paginacion() {
    }

    // Crea una solicitud de página con valores corregidos y un orden fijo definido por el servidor
    public static PageRequest de(int pagina, int tamano, Sort orden) {
        // La página no puede ser negativa y el tamaño debe estar entre 1 y el máximo
        return PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), TAMANO_MAXIMO), orden);
    }
}
