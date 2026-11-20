// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Validación: el texto no puede estar vacío
import jakarta.validation.constraints.NotBlank;
// Validación: el texto debe tener una longitud permitida
import jakarta.validation.constraints.Size;

// Datos que envía el líder para sumar a una persona al grupo
public record IntegranteRequest(
        // El correo es obligatorio
        @NotBlank(message = "El correo es obligatorio")
        // El correo no puede pasar de 150 caracteres
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        // Correo de la persona que se quiere invitar
        String correo) {
}
