// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Validación: el texto no puede estar vacío
import jakarta.validation.constraints.NotBlank;
// Validación: el texto debe tener una longitud permitida
import jakarta.validation.constraints.Size;

// Datos que envía Angular para iniciar sesión
public record LoginRequest(
        // El correo es obligatorio
        @NotBlank(message = "El correo es obligatorio")
        // El correo no puede pasar de 150 caracteres
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        // Correo electrónico
        String correo,

        // La contraseña es obligatoria
        @NotBlank(message = "La contraseña es obligatoria")
        // Se limita el tamaño para no procesar textos enormes
        @Size(max = 72, message = "La contraseña no puede superar los 72 caracteres")
        // Contraseña en texto plano
        String contrasena) {
}
