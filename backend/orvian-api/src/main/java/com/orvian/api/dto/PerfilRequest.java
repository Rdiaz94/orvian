// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Validación: el número no puede superar un máximo
import jakarta.validation.constraints.Max;
// Validación: el número no puede ser menor a un mínimo
import jakarta.validation.constraints.Min;
// Validación: el texto no puede estar vacío
import jakarta.validation.constraints.NotBlank;
// Validación: el texto debe tener una longitud permitida
import jakarta.validation.constraints.Size;

// Datos que el usuario puede cambiar de su perfil; el correo y el rol nunca se editan desde aquí
public record PerfilRequest(
        // El nombre es obligatorio
        @NotBlank(message = "El nombre es obligatorio")
        // El nombre no puede pasar de 100 caracteres
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        // Nombre completo
        String nombre,

        // La carrera no puede pasar de 100 caracteres
        @Size(max = 100, message = "La carrera no puede superar los 100 caracteres")
        // Carrera que estudia (opcional)
        String carrera,

        // El ciclo mínimo es 1
        @Min(value = 1, message = "El ciclo debe ser mayor o igual a 1")
        // El ciclo máximo es 12
        @Max(value = 12, message = "El ciclo no puede ser mayor a 12")
        // Ciclo académico (opcional)
        Integer ciclo) {
}
