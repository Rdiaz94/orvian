// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Validación: el texto no puede estar vacío
import jakarta.validation.constraints.NotBlank;
// Validación: el texto debe tener una longitud permitida
import jakarta.validation.constraints.Size;

// Datos que envía el administrador para crear un curso
public record CursoRequest(
        // El nombre es obligatorio
        @NotBlank(message = "El nombre del curso es obligatorio")
        // El nombre no puede pasar de 100 caracteres
        @Size(max = 100, message = "El nombre del curso no puede superar los 100 caracteres")
        // Nombre del curso
        String nombre) {
}
