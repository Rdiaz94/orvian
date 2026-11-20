// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Validación: el número no puede superar un máximo
import jakarta.validation.constraints.Max;
// Validación: el número no puede ser menor a un mínimo
import jakarta.validation.constraints.Min;
// Validación: el texto debe tener formato de correo
import jakarta.validation.constraints.Email;
// Validación: el texto no puede estar vacío
import jakarta.validation.constraints.NotBlank;
// Validación: el texto debe tener una longitud permitida
import jakarta.validation.constraints.Size;

// Datos que envía Angular para registrar un usuario nuevo
public record RegistroRequest(
        // El nombre es obligatorio
        @NotBlank(message = "El nombre es obligatorio")
        // El nombre no puede pasar de 100 caracteres
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        // Nombre completo
        String nombre,

        // El correo es obligatorio
        @NotBlank(message = "El correo es obligatorio")
        // El correo debe tener un formato válido
        @Email(message = "El correo no tiene un formato válido")
        // El correo no puede pasar de 150 caracteres
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        // Correo electrónico
        String correo,

        // La contraseña es obligatoria
        @NotBlank(message = "La contraseña es obligatoria")
        // Mínimo 8 caracteres y máximo 72, que es el límite que procesa BCrypt
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        // Contraseña en texto plano; solo viaja hasta el servidor y se cifra al instante
        String contrasena,

        // La carrera no puede pasar de 100 caracteres
        @Size(max = 100, message = "La carrera no puede superar los 100 caracteres")
        // Carrera que estudia el usuario (opcional)
        String carrera,

        // El ciclo mínimo es 1
        @Min(value = 1, message = "El ciclo debe ser mayor o igual a 1")
        // El ciclo máximo es 12
        @Max(value = 12, message = "El ciclo no puede ser mayor a 12")
        // Ciclo académico (opcional)
        Integer ciclo) {
}
