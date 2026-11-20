// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Entidad de la que se toman los datos
import com.orvian.api.entity.Curso;

// Datos públicos de un curso
public record CursoResponse(
        // Identificador del curso
        Long id,
        // Nombre del curso
        String nombre) {

    // Convierte una entidad en su versión pública
    public static CursoResponse desde(Curso curso) {
        // Copia solo los campos que se pueden exponer
        return new CursoResponse(curso.getId(), curso.getNombre());
    }
}
