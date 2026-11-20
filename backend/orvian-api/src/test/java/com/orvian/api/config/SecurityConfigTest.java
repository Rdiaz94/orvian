// Paquete de las pruebas de la configuración
package com.orvian.api.config;

// Verificaciones legibles de AssertJ
import static org.assertj.core.api.Assertions.assertThat;

// Anotación que marca un método como prueba
import org.junit.jupiter.api.Test;
// Interfaz común de los cifradores de contraseñas
import org.springframework.security.crypto.password.PasswordEncoder;

// Pruebas del cifrado de contraseñas, sin levantar Spring
class SecurityConfigTest {

    // Cifrador que se va a probar
    private final PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

    // Marca el método como prueba
    @Test
    // La contraseña cifrada no debe ser igual al texto original
    void cifraLaContrasenaYNoLaGuardaEnTextoPlano() {
        // Contraseña de ejemplo
        String clave = "Clave-Segura-123";
        // Cifra la contraseña
        String hash = encoder.encode(clave);
        // Comprueba que el hash es distinto de la contraseña
        assertThat(hash).isNotEqualTo(clave);
        // Comprueba que el hash tiene el formato BCrypt
        assertThat(hash).startsWith("$2");
    }

    // Marca el método como prueba
    @Test
    // El cifrador debe reconocer la contraseña correcta y rechazar una incorrecta
    void verificaLaContrasenaCorrectaYRechazaLaIncorrecta() {
        // Contraseña de ejemplo
        String clave = "Clave-Segura-123";
        // Cifra la contraseña
        String hash = encoder.encode(clave);
        // La contraseña correcta debe coincidir
        assertThat(encoder.matches(clave, hash)).isTrue();
        // Una contraseña distinta no debe coincidir
        assertThat(encoder.matches("otra-clave", hash)).isFalse();
    }

    // Marca el método como prueba
    @Test
    // Dos cifrados de la misma contraseña deben dar resultados distintos por la sal aleatoria
    void generaHashesDistintosParaLaMismaContrasena() {
        // Contraseña de ejemplo
        String clave = "Clave-Segura-123";
        // Los dos hashes no deben ser iguales
        assertThat(encoder.encode(clave)).isNotEqualTo(encoder.encode(clave));
    }
}
