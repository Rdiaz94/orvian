// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Validación: el texto no puede estar vacío
import jakarta.validation.constraints.NotBlank;
// Validación: el texto debe tener una longitud permitida
import jakarta.validation.constraints.Size;

// Datos para cambiar la contraseña; exige la actual para que un token robado no baste para tomar la cuenta
public record CambioContrasenaRequest(
        // La contraseña actual es obligatoria
        @NotBlank(message = "La contraseña actual es obligatoria")
        // Se limita el tamaño para no procesar textos enormes
        @Size(max = 72, message = "La contraseña actual no puede superar los 72 caracteres")
        // Contraseña actual en texto plano
        String contrasenaActual,

        // La contraseña nueva es obligatoria
        @NotBlank(message = "La contraseña nueva es obligatoria")
        // Entre 8 y 72 caracteres, que es lo que procesa BCrypt
        @Size(min = 8, max = 72, message = "La contraseña nueva debe tener entre 8 y 72 caracteres")
        // Contraseña nueva en texto plano
        String contrasenaNueva) {
}
