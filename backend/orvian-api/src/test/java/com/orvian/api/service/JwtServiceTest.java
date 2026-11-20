// Paquete de las pruebas de la capa de servicio
package com.orvian.api.service;

// Verificaciones legibles de AssertJ
import static org.assertj.core.api.Assertions.assertThat;
// Verificación de excepciones de AssertJ
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Reloj que se puede fijar en una fecha concreta
import java.time.Clock;
// Intervalo de tiempo
import java.time.Duration;
// Importa la fecha y hora con precisión de instante
import java.time.Instant;
// Zona horaria UTC
import java.time.ZoneOffset;

// Clave secreta de la librería de criptografía de Java
import javax.crypto.SecretKey;

// Anotación que ejecuta un método antes de cada prueba
import org.junit.jupiter.api.BeforeEach;
// Anotación que marca un método como prueba
import org.junit.jupiter.api.Test;
// Decodificador de tokens
import org.springframework.security.oauth2.jwt.Jwt;
// Excepción base de los tokens inválidos
import org.springframework.security.oauth2.jwt.JwtException;
// Decodificador de tokens
import org.springframework.security.oauth2.jwt.JwtDecoder;
// Utilidad para asignar campos privados en pruebas
import org.springframework.test.util.ReflectionTestUtils;

// Configuración de la firma de tokens
import com.orvian.api.config.JwtConfig;
// Ajustes de los tokens
import com.orvian.api.config.JwtProperties;
// Entidad de usuario
import com.orvian.api.entity.Usuario;

// Pruebas de la emisión y verificación de tokens, sin levantar Spring
class JwtServiceTest {

    // Secreto de prueba de 32 bytes en Base64
    private static final String SECRETO = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    // Otro secreto distinto para simular un atacante
    private static final String OTRO_SECRETO = "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVowMTIzNDU=";

    // Configuración real de firma
    private final JwtConfig config = new JwtConfig();
    // Ajustes de prueba: tokens de 60 minutos
    private final JwtProperties propiedades = new JwtProperties(SECRETO, 60);
    // Clave derivada del secreto
    private final SecretKey clave = config.claveJwt(propiedades);
    // Decodificador real que verifica los tokens
    private final JwtDecoder decodificador = config.jwtDecoder(clave);
    // Usuario de ejemplo
    private Usuario usuario;

    // Se ejecuta antes de cada prueba
    @BeforeEach
    // Prepara un usuario con identificador
    void preparar() {
        // Crea un usuario de ejemplo
        usuario = new Usuario("Ana", "ana@correo.com", "hash", "Ingeniería", 5);
        // Asigna el identificador, que normalmente pone la base de datos
        ReflectionTestUtils.setField(usuario, "id", 7L);
    }

    // Crea un servicio de tokens con la clave y el reloj indicados
    private JwtService servicio(JwtProperties ajustes, Clock reloj) {
        // Construye el servicio con un codificador real
        return new JwtService(config.jwtEncoder(config.claveJwt(ajustes)), ajustes, reloj);
    }

    // Marca el método como prueba
    @Test
    // El token debe llevar la identidad, el rol, el emisor y una expiración posterior a la emisión
    void emiteUnTokenValidoConIdentidadYRol() {
        // Emite el token con el reloj real
        TokenEmitido token = servicio(propiedades, Clock.systemUTC()).emitir(usuario);
        // Lo decodifica con la verificación completa
        Jwt jwt = decodificador.decode(token.valor());
        // El sujeto es el identificador del usuario
        assertThat(jwt.getSubject()).isEqualTo("7");
        // El rol viaja dentro del token
        assertThat(jwt.getClaimAsString("rol")).isEqualTo("ESTUDIANTE");
        // El emisor es Orvian
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(JwtConfig.EMISOR);
        // El token expira después de emitirse
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
        // El token no incluye datos sensibles como el hash de la contraseña
        assertThat(jwt.getClaims()).doesNotContainKeys("contrasenaHash", "contrasena", "correo");
    }

    // Marca el método como prueba
    @Test
    // Un token alterado debe ser rechazado
    void rechazaUnTokenManipulado() {
        // Emite un token válido
        String valor = servicio(propiedades, Clock.systemUTC()).emitir(usuario).valor();
        // Cambia los últimos caracteres de la firma
        String manipulado = valor.substring(0, valor.length() - 2) + "xx";
        // El decodificador debe rechazarlo
        assertThatThrownBy(() -> decodificador.decode(manipulado)).isInstanceOf(JwtException.class);
    }

    // Marca el método como prueba
    @Test
    // Un token firmado con otra clave debe ser rechazado
    void rechazaUnTokenFirmadoConOtraClave() {
        // Ajustes con un secreto distinto
        JwtProperties ajenos = new JwtProperties(OTRO_SECRETO, 60);
        // Emite un token con la clave del atacante
        String falso = servicio(ajenos, Clock.systemUTC()).emitir(usuario).valor();
        // El decodificador legítimo debe rechazarlo
        assertThatThrownBy(() -> decodificador.decode(falso)).isInstanceOf(JwtException.class);
    }

    // Marca el método como prueba
    @Test
    // Un token expirado debe ser rechazado
    void rechazaUnTokenExpirado() {
        // Reloj fijo en un momento de hace tres horas
        Clock pasado = Clock.fixed(Instant.now().minus(Duration.ofHours(3)), ZoneOffset.UTC);
        // Emite un token que duraba 60 minutos y ya venció
        String vencido = servicio(propiedades, pasado).emitir(usuario).valor();
        // El decodificador debe rechazarlo por expiración
        assertThatThrownBy(() -> decodificador.decode(vencido)).isInstanceOf(JwtException.class);
    }
}
