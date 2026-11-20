// Paquete de la capa de lógica de negocio
package com.orvian.api.service;

// Intervalo de tiempo
import java.time.Duration;
// Importa la fecha y hora con precisión de instante
import java.time.Instant;
// Contenedor para un valor que puede no existir
import java.util.Optional;

// Excepción de Spring cuando se viola una restricción de la base de datos
import org.springframework.dao.DataIntegrityViolationException;
// Cifrador de contraseñas
import org.springframework.security.crypto.password.PasswordEncoder;
// Anotación que marca la clase como servicio
import org.springframework.stereotype.Service;
// Anotación que ejecuta el método dentro de una transacción
import org.springframework.transaction.annotation.Transactional;

// Respuesta del inicio de sesión
import com.orvian.api.dto.AuthResponse;
// Datos de entrada del inicio de sesión
import com.orvian.api.dto.LoginRequest;
// Datos de entrada del registro
import com.orvian.api.dto.RegistroRequest;
// Datos públicos del usuario
import com.orvian.api.dto.UsuarioResponse;
// Entidad de usuario
import com.orvian.api.entity.Usuario;
// Excepción de correo repetido
import com.orvian.api.exception.CorreoDuplicadoException;
// Excepción de credenciales incorrectas
import com.orvian.api.exception.CredencialesInvalidasException;
// Repositorio de usuarios
import com.orvian.api.repository.UsuarioRepository;
// Reglas comunes para tratar correos
import com.orvian.api.util.Correos;

// Lógica de negocio de la autenticación
@Service
// Clase que registra usuarios, inicia sesiones y consulta el perfil
public class AuthService {

    // Acceso a los datos de usuarios
    private final UsuarioRepository usuarios;
    // Cifrador de contraseñas
    private final PasswordEncoder cifrador;
    // Servicio que crea los tokens
    private final JwtService jwt;
    // Frena los intentos de fuerza bruta
    private final LimiteIntentos limite;
    // Hash de una contraseña inventada: sirve para tardar lo mismo cuando el correo no existe
    private final String hashFalso;

    // Spring entrega las dependencias por el constructor, lo que facilita las pruebas
    public AuthService(UsuarioRepository usuarios, PasswordEncoder cifrador, JwtService jwt, LimiteIntentos limite) {
        // Guarda el repositorio
        this.usuarios = usuarios;
        // Guarda el cifrador
        this.cifrador = cifrador;
        // Guarda el servicio de tokens
        this.jwt = jwt;
        // Guarda el limitador de intentos
        this.limite = limite;
        // Calcula una sola vez el hash falso
        this.hashFalso = cifrador.encode("contrasena-falsa-para-igualar-tiempos");
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Registra un usuario nuevo y devuelve sus datos públicos
    public UsuarioResponse registrar(RegistroRequest datos) {
        // Normaliza el correo para que Ana@x.com y ana@x.com sean el mismo
        String correo = Correos.normalizar(datos.correo());
        // Si el correo ya existe, se rechaza el registro
        if (usuarios.existsByCorreo(correo)) {
            // Lanza la excepción de correo repetido
            throw new CorreoDuplicadoException(correo);
        }
        // Crea el usuario con la contraseña ya cifrada
        Usuario usuario = new Usuario(datos.nombre().trim(), correo, cifrador.encode(datos.contrasena()), datos.carrera(), datos.ciclo());
        // Intenta guardar el usuario
        try {
            // Guarda y devuelve solo los datos públicos
            return UsuarioResponse.desde(usuarios.save(usuario));
        // Si dos registros simultáneos chocan, la restricción única de la base de datos lo detecta
        } catch (DataIntegrityViolationException error) {
            // Se trata igual que un correo repetido
            throw new CorreoDuplicadoException(correo);
        }
    }

    // Solo lee datos, así que la transacción es de solo lectura
    @Transactional(readOnly = true)
    // Verifica las credenciales y devuelve un token de acceso
    public AuthResponse login(LoginRequest datos, String ip) {
        // Normaliza el correo para que el límite y la búsqueda usen el mismo valor
        String correo = Correos.normalizar(datos.correo());
        // Si ya se superó el límite de intentos, se rechaza antes de consultar nada
        limite.exigirPermitido(ip, correo);
        // Busca al usuario por su correo normalizado
        Optional<Usuario> encontrado = usuarios.findByCorreo(correo);
        // Si el usuario no existe, se compara contra el hash falso para tardar lo mismo
        String hash = encontrado.map(Usuario::getContrasenaHash).orElse(hashFalso);
        // Compara la contraseña recibida con el hash
        boolean coincide = cifrador.matches(datos.contrasena(), hash);
        // Correo inexistente y contraseña errónea producen exactamente el mismo error
        if (encontrado.isEmpty() || !coincide) {
            // Anota el fallo para el límite de intentos
            limite.registrarFallo(ip, correo);
            // Lanza la excepción de credenciales incorrectas
            throw new CredencialesInvalidasException();
        }
        // Un inicio de sesión correcto reinicia el contador de esa cuenta
        limite.registrarExito(ip, correo);
        // Obtiene el usuario autenticado
        Usuario usuario = encontrado.get();
        // Crea el token de acceso
        TokenEmitido token = jwt.emitir(usuario);
        // Calcula los segundos de vigencia que le quedan al token
        long vigencia = Duration.between(Instant.now(), token.expiraEn()).toSeconds();
        // Devuelve el token con los datos públicos del usuario
        return new AuthResponse(token.valor(), "Bearer", vigencia, UsuarioResponse.desde(usuario));
    }

    // Solo lee datos, así que la transacción es de solo lectura
    @Transactional(readOnly = true)
    // Devuelve el perfil del usuario dueño del token
    public UsuarioResponse perfil(Long id) {
        // Si el usuario ya no existe, el token deja de ser útil
        return usuarios.findById(id).map(UsuarioResponse::desde).orElseThrow(CredencialesInvalidasException::new);
    }
}
