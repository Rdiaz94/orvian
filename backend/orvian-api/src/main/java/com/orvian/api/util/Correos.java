// Paquete de las utilidades compartidas
package com.orvian.api.util;

// Utilidad para convertir texto a minúsculas sin depender del idioma del equipo
import java.util.Locale;

// Reglas comunes para tratar correos electrónicos
public final class Correos {

    // Impide crear instancias: la clase solo ofrece métodos estáticos
    private Correos() {
    }

    // Quita espacios y pasa a minúsculas para comparar correos de forma uniforme
    public static String normalizar(String correo) {
        // Aplica la normalización
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
