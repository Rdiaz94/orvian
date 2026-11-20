// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Respuesta del inicio de sesión
public record AuthResponse(
        // Token JWT que Angular enviará en cada petición
        String token,
        // Tipo de token; con Bearer se envía en la cabecera Authorization
        String tipo,
        // Segundos que faltan para que el token expire
        long expiraEnSegundos,
        // Datos públicos del usuario que inició sesión
        UsuarioResponse usuario) {
}
