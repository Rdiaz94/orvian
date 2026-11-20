// Paquete de la capa de lógica de negocio
package com.orvian.api.service;

// Importa la fecha y hora con precisión de instante
import java.time.Instant;

// Token recién creado junto con el momento en que expira
public record TokenEmitido(
        // Texto del token JWT firmado
        String valor,
        // Momento en que el token deja de ser válido
        Instant expiraEn) {
}
