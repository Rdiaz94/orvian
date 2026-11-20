// Paquete de las clases de configuración
package com.orvian.api.config;

// Enlaza propiedades del archivo de configuración con esta clase
import org.springframework.boot.context.properties.ConfigurationProperties;
// Activa la validación de las propiedades al arrancar
import org.springframework.validation.annotation.Validated;

// Validación: el número no puede superar un máximo
import jakarta.validation.constraints.Max;
// Validación: el número no puede ser menor a un mínimo
import jakarta.validation.constraints.Min;
// Validación: el texto no puede estar vacío
import jakarta.validation.constraints.NotBlank;

// Valida las propiedades: si falta alguna, la aplicación no arranca
@Validated
// Toma las propiedades que empiezan con orvian.seguridad.jwt
@ConfigurationProperties(prefix = "orvian.seguridad.jwt")
// Ajustes de los tokens JWT
public record JwtProperties(
        // El secreto es obligatorio y se explica qué hacer si falta
        @NotBlank(message = "Falta el secreto JWT: define la variable ORVIAN_JWT_SECRETO o orvian.seguridad.jwt.secreto")
        // Secreto en Base64 con el que se firman los tokens
        String secreto,

        // La duración mínima es 1 minuto
        @Min(1)
        // La duración máxima es un día
        @Max(1440)
        // Minutos de vigencia de cada token
        long expiracionMinutos) {
}
