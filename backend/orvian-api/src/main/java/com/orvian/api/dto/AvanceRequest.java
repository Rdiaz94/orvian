// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Estado de la tarea
import com.orvian.api.entity.EstadoTarea;
// Validación: el número no puede superar un máximo
import jakarta.validation.constraints.Max;
// Validación: el número no puede ser menor a un mínimo
import jakarta.validation.constraints.Min;
// Validación: el valor no puede ser nulo
import jakarta.validation.constraints.NotNull;

// Datos que envía el usuario para mover una tarea de columna o actualizar su avance
public record AvanceRequest(
        // El estado es obligatorio
        @NotNull(message = "El estado es obligatorio")
        // Nuevo estado de la tarea
        EstadoTarea estado,

        // El avance mínimo es 0
        @Min(value = 0, message = "El avance no puede ser negativo")
        // El avance máximo es 100
        @Max(value = 100, message = "El avance no puede superar 100")
        // Porcentaje de avance; solo se usa cuando la tarea está en progreso
        Integer progreso) {
}
