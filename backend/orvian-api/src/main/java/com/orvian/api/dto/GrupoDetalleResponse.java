// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Lista de elementos
import java.util.List;

// Detalle de un grupo con todos sus integrantes; solo lo ven los propios integrantes
public record GrupoDetalleResponse(
        // Datos del grupo
        GrupoResponse grupo,
        // Integrantes del grupo
        List<IntegranteResponse> integrantes) {
}
