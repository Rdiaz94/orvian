// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Tipo de recurso
import com.orvian.api.entity.TipoRecurso;
// Validación: el texto no puede estar vacío
import jakarta.validation.constraints.NotBlank;
// Validación: el valor no puede ser nulo
import jakarta.validation.constraints.NotNull;
// Validación: el texto debe cumplir una expresión regular
import jakarta.validation.constraints.Pattern;
// Validación: el texto debe tener una longitud permitida
import jakarta.validation.constraints.Size;

// Datos que envía un usuario para compartir o editar un recurso; el autor nunca viaja aquí
public record RecursoRequest(
        // El título es obligatorio
        @NotBlank(message = "El título es obligatorio")
        // El título no puede pasar de 150 caracteres
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        // Título del recurso
        String titulo,

        // El tipo es obligatorio
        @NotNull(message = "El tipo es obligatorio")
        // Tipo de recurso
        TipoRecurso tipo,

        // El enlace es obligatorio
        @NotBlank(message = "El enlace es obligatorio")
        // El enlace no puede pasar de 500 caracteres
        @Size(max = 500, message = "El enlace no puede superar los 500 caracteres")
        // Solo se admiten http y https sin espacios: así se bloquean enlaces peligrosos como javascript:
        @Pattern(regexp = "(?i)^https?://\\S+$", message = "El enlace debe empezar con http:// o https:// y no tener espacios")
        // Enlace donde está alojado el recurso
        String url,

        // La descripción no puede pasar de 500 caracteres
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        // Descripción breve (opcional)
        String descripcion,

        // Identificador del curso (opcional)
        Long cursoId,

        // Grupo con el que se comparte; si falta, el recurso es público (opcional)
        Long grupoId) {
}
