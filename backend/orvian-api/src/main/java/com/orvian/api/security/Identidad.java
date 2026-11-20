// Paquete de las utilidades de seguridad
package com.orvian.api.security;

// Token JWT ya validado
import org.springframework.security.oauth2.jwt.Jwt;

// Obtiene quién hace la petición a partir de su token
public final class Identidad {

    // Impide crear instancias: la clase solo ofrece métodos estáticos
    private Identidad() {
    }

    // El sujeto del token es el identificador del usuario; nunca se toma de datos que envíe el cliente
    public static Long idDe(Jwt token) {
        // Convierte el sujeto del token en número
        return Long.valueOf(token.getSubject());
    }
}
