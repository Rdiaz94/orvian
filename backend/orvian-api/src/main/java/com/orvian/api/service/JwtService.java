// Paquete de la capa de lógica de negocio
package com.orvian.api.service;

// Reloj que se inyecta para poder probar la expiración
import java.time.Clock;
// Intervalo de tiempo
import java.time.Duration;
// Importa la fecha y hora con precisión de instante
import java.time.Instant;

// Algoritmo de firma HMAC
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
// Cabecera del token con el algoritmo de firma
import org.springframework.security.oauth2.jwt.JwsHeader;
// Datos que contiene el token
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
// Codificador de tokens
import org.springframework.security.oauth2.jwt.JwtEncoder;
// Parámetros para firmar un token
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
// Anotación que marca la clase como servicio
import org.springframework.stereotype.Service;

// Configuración del emisor del token
import com.orvian.api.config.JwtConfig;
// Ajustes de duración del token
import com.orvian.api.config.JwtProperties;
// Entidad de usuario
import com.orvian.api.entity.Usuario;

// Crea los tokens JWT de acceso
@Service
// Clase que firma tokens con la identidad y el rol del usuario
public class JwtService {

    // Componente que firma los tokens
    private final JwtEncoder codificador;
    // Ajustes con la duración del token
    private final JwtProperties propiedades;
    // Reloj del sistema
    private final Clock reloj;

    // Spring entrega las dependencias por el constructor
    public JwtService(JwtEncoder codificador, JwtProperties propiedades, Clock reloj) {
        // Guarda el codificador
        this.codificador = codificador;
        // Guarda los ajustes
        this.propiedades = propiedades;
        // Guarda el reloj
        this.reloj = reloj;
    }

    // Crea un token firmado para el usuario indicado
    public TokenEmitido emitir(Usuario usuario) {
        // Momento actual
        Instant ahora = Instant.now(reloj);
        // Momento en que expira el token
        Instant expira = ahora.plus(Duration.ofMinutes(propiedades.expiracionMinutos()));
        // Arma el contenido del token sin incluir datos sensibles
        JwtClaimsSet datos = JwtClaimsSet.builder()
                // Quién emite el token
                .issuer(JwtConfig.EMISOR)
                // Cuándo se emitió
                .issuedAt(ahora)
                // Cuándo expira
                .expiresAt(expira)
                // A quién pertenece: el identificador del usuario
                .subject(String.valueOf(usuario.getId()))
                // Rol del usuario para autorizar por permisos
                .claim("rol", usuario.getRol().name())
                // Termina de construir el contenido
                .build();
        // Cabecera que indica el algoritmo de firma
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        // Firma el token y obtiene su texto
        String valor = codificador.encode(JwtEncoderParameters.from(cabecera, datos)).getTokenValue();
        // Devuelve el token junto con su expiración
        return new TokenEmitido(valor, expira);
    }
}
