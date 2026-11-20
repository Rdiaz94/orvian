// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Validación: el texto no puede estar vacío
import jakarta.validation.constraints.NotBlank;
// Validación: el texto debe tener una longitud permitida
import jakarta.validation.constraints.Size;

// Datos que envía el usuario para crear un grupo; el creador siempre es quien tiene la sesión iniciada
public record GrupoRequest(
        // El nombre es obligatorio
        @NotBlank(message = "El nombre del grupo es obligatorio")
        // El nombre no puede pasar de 100 caracteres
        @Size(max = 100, message = "El nombre del grupo no puede superar los 100 caracteres")
        // Nombre del grupo
        String nombre,

        // La descripción no puede pasar de 500 caracteres
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        // Descripción del grupo (opcional)
        String descripcion,

        // Identificador del curso asociado (opcional)
        Long cursoId) {
}
