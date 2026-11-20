// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Importa la fecha y hora con precisión de instante
import java.time.Instant;

// Etiqueta de la tarea
import com.orvian.api.entity.EtiquetaTarea;
// Validación: el texto no puede estar vacío
import jakarta.validation.constraints.NotBlank;
// Validación: el texto debe tener una longitud permitida
import jakarta.validation.constraints.Size;

// Datos que envía el usuario para crear o editar una tarea; el dueño nunca viaja aquí
public record TareaRequest(
        // El título es obligatorio
        @NotBlank(message = "El título es obligatorio")
        // El título no puede pasar de 150 caracteres
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        // Título de la tarea
        String titulo,

        // La descripción no puede pasar de 1000 caracteres
        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        // Descripción de la tarea (opcional)
        String descripcion,

        // Identificador del curso (opcional)
        Long cursoId,

        // Etiqueta de la tarea (opcional; si falta se usa OTRO)
        EtiquetaTarea etiqueta,

        // Fecha límite de entrega (opcional)
        Instant fechaVencimiento) {
}
