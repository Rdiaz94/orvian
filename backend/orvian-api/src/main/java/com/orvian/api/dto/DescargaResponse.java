// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Respuesta al registrar una descarga: el enlace que el cliente debe abrir
public record DescargaResponse(
        // Enlace del recurso
        String url) {
}
