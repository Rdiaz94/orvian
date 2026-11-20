// Paquete de las clases de configuración
package com.orvian.api.config;

// Reloj del sistema, que se inyecta para poder probar la expiración
import java.time.Clock;
// Utilidad para decodificar texto en Base64
import java.util.Base64;

// Clave secreta de la librería de criptografía de Java
import javax.crypto.SecretKey;
// Implementación de clave secreta a partir de bytes
import javax.crypto.spec.SecretKeySpec;

// Origen de la clave que usa Nimbus para firmar
import com.nimbusds.jose.jwk.source.ImmutableSecret;
// Registra componentes que Spring administra
import org.springframework.context.annotation.Bean;
// Anotación que marca la clase como fuente de configuración
import org.springframework.context.annotation.Configuration;
// Activa la lectura de JwtProperties desde el archivo de configuración
import org.springframework.boot.context.properties.EnableConfigurationProperties;
// Algoritmo de firma HMAC
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
// Decodificador de tokens
import org.springframework.security.oauth2.jwt.JwtDecoder;
// Codificador de tokens
import org.springframework.security.oauth2.jwt.JwtEncoder;
// Fábrica de validadores de tokens
import org.springframework.security.oauth2.jwt.JwtValidators;
// Decodificador basado en la librería Nimbus
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
// Codificador basado en la librería Nimbus
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

// Indica que esta clase define componentes de configuración
@Configuration
// Habilita las propiedades de JWT
@EnableConfigurationProperties(JwtProperties.class)
// Configura cómo se firman y verifican los tokens
public class JwtConfig {

    // Identifica quién emite los tokens y se exige al validarlos
    public static final String EMISOR = "orvian-api";
    // HS256 exige una clave de al menos 256 bits, es decir, 32 bytes
    private static final int BYTES_MINIMOS = 32;

    // Registra el reloj que usa el sistema
    @Bean
    // Método que crea el reloj en UTC
    public Clock reloj() {
        // Devuelve el reloj del sistema en UTC
        return Clock.systemUTC();
    }

    // Registra la clave secreta con la que se firman los tokens
    @Bean
    // Convierte el secreto Base64 en una clave y verifica su fortaleza
    public SecretKey claveJwt(JwtProperties propiedades) {
        // Bytes del secreto
        byte[] bytes;
        // Intenta decodificar el secreto
        try {
            // Decodifica el texto Base64
            bytes = Base64.getDecoder().decode(propiedades.secreto());
        // Si el texto no es Base64 válido
        } catch (IllegalArgumentException error) {
            // Aborta el arranque sin mostrar el secreto
            throw new IllegalStateException("El secreto JWT debe estar codificado en Base64", error);
        }
        // Si la clave es demasiado corta, se aborta el arranque
        if (bytes.length < BYTES_MINIMOS) {
            // Mensaje sin revelar el valor del secreto
            throw new IllegalStateException("El secreto JWT debe tener al menos 256 bits (32 bytes)");
        }
        // Crea la clave para el algoritmo HMAC-SHA256
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    // Registra el componente que firma los tokens
    @Bean
    // Crea el codificador a partir de la clave
    public JwtEncoder jwtEncoder(SecretKey claveJwt) {
        // Usa Nimbus con la clave secreta inmutable
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
    }

    // Registra el componente que verifica los tokens
    @Bean
    // Crea el decodificador a partir de la clave
    public JwtDecoder jwtDecoder(SecretKey claveJwt) {
        // Construye el decodificador con el mismo algoritmo de firma
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(claveJwt).macAlgorithm(MacAlgorithm.HS256).build();
        // Exige firma válida, no expirado y emitido por Orvian
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISOR));
        // Devuelve el decodificador configurado
        return decodificador;
    }
}
