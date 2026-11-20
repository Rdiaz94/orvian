// Paquete de las pruebas de la capa de servicio
package com.orvian.api.service;

// Verificaciones legibles de AssertJ
import static org.assertj.core.api.Assertions.assertThat;
// Verificación de excepciones de AssertJ
import static org.assertj.core.api.Assertions.assertThatThrownBy;
// Comodín para cualquier argumento en Mockito
import static org.mockito.ArgumentMatchers.any;
// Hace que un método simulado lance una excepción
import static org.mockito.Mockito.doThrow;
// Verifica que un método nunca se llamó
import static org.mockito.Mockito.never;
// Verifica que un objeto simulado no se usó
import static org.mockito.Mockito.verifyNoInteractions;
// Verifica que un método se llamó
import static org.mockito.Mockito.verify;
// Define el comportamiento de un objeto simulado
import static org.mockito.Mockito.when;

// Importa la fecha y hora con precisión de instante
import java.time.Instant;
// Contenedor para un valor que puede no existir
import java.util.Optional;

// Anotación que marca un método como prueba
import org.junit.jupiter.api.Test;
// Anotación para ejecutar la prueba con Mockito
import org.junit.jupiter.api.extension.ExtendWith;
// Captura el argumento con el que se llamó a un método
import org.mockito.ArgumentCaptor;
// Crea un objeto simulado
import org.mockito.Mock;
// Inyecta los simulados en la clase a probar
import org.mockito.InjectMocks;
// Extensión de Mockito para JUnit 5
import org.mockito.junit.jupiter.MockitoExtension;
// Cifrador de contraseñas
import org.springframework.security.crypto.password.PasswordEncoder;

// Respuesta del inicio de sesión
import com.orvian.api.dto.AuthResponse;
// Datos de entrada del inicio de sesión
import com.orvian.api.dto.LoginRequest;
// Datos de entrada del registro
import com.orvian.api.dto.RegistroRequest;
// Datos públicos del usuario
import com.orvian.api.dto.UsuarioResponse;
// Rol del usuario
import com.orvian.api.entity.Rol;
// Entidad de usuario
import com.orvian.api.entity.Usuario;
// Excepción de correo repetido
import com.orvian.api.exception.CorreoDuplicadoException;
// Excepción de credenciales incorrectas
import com.orvian.api.exception.CredencialesInvalidasException;
// Excepción por exceso de intentos
import com.orvian.api.exception.DemasiadosIntentosException;
// Repositorio de usuarios
import com.orvian.api.repository.UsuarioRepository;

// Activa Mockito en esta clase de pruebas
@ExtendWith(MockitoExtension.class)
// Pruebas del servicio de autenticación sin base de datos
class AuthServiceTest {

    // Repositorio simulado: no toca la base de datos
    @Mock
    private UsuarioRepository usuarios;

    // Cifrador simulado
    @Mock
    private PasswordEncoder cifrador;

    // Servicio de tokens simulado
    @Mock
    private JwtService jwt;

    // Limitador de intentos simulado
    @Mock
    private LimiteIntentos limite;

    // Dirección IP de las pruebas
    private static final String IP = "127.0.0.1";

    // Servicio real al que se le inyectan los simulados
    @InjectMocks
    private AuthService servicio;

    // Datos de registro con correo en mayúsculas y espacios
    private final RegistroRequest registro = new RegistroRequest(" Ana Pérez ", "  ANA@Correo.com ", "Clave-123456", "Ingeniería de Sistemas Computacionales", 5);
    // Datos de inicio de sesión con el correo sin normalizar
    private final LoginRequest login = new LoginRequest(" Ana@Correo.com ", "Clave-123456");

    // Marca el método como prueba
    @Test
    // Debe normalizar el correo, cifrar la contraseña y asignar rol de estudiante
    void registraUsuarioConCorreoNormalizadoYContrasenaCifrada() {
        // El correo normalizado todavía no existe
        when(usuarios.existsByCorreo("ana@correo.com")).thenReturn(false);
        // El cifrador devuelve un hash de ejemplo
        when(cifrador.encode("Clave-123456")).thenReturn("hash-de-ejemplo");
        // El repositorio devuelve el mismo usuario que recibe
        when(usuarios.save(any(Usuario.class))).thenAnswer(llamada -> llamada.getArgument(0));

        // Ejecuta el registro
        UsuarioResponse respuesta = servicio.registrar(registro);

        // Captura el usuario que se intentó guardar
        ArgumentCaptor<Usuario> captura = ArgumentCaptor.forClass(Usuario.class);
        // Verifica que se guardó una vez y obtiene el usuario
        verify(usuarios).save(captura.capture());
        // El correo se guardó normalizado
        assertThat(captura.getValue().getCorreo()).isEqualTo("ana@correo.com");
        // El nombre se guardó sin espacios sobrantes
        assertThat(captura.getValue().getNombre()).isEqualTo("Ana Pérez");
        // Se guardó el hash y no la contraseña
        assertThat(captura.getValue().getContrasenaHash()).isEqualTo("hash-de-ejemplo");
        // El rol por defecto es estudiante
        assertThat(captura.getValue().getRol()).isEqualTo(Rol.ESTUDIANTE);
        // La respuesta pública trae el correo normalizado
        assertThat(respuesta.correo()).isEqualTo("ana@correo.com");
    }

    // Marca el método como prueba
    @Test
    // Debe rechazar un correo que ya existe y no guardar nada
    void rechazaUnCorreoDuplicado() {
        // El correo normalizado ya existe
        when(usuarios.existsByCorreo("ana@correo.com")).thenReturn(true);

        // El registro debe fallar con la excepción de correo repetido
        assertThatThrownBy(() -> servicio.registrar(registro)).isInstanceOf(CorreoDuplicadoException.class);
        // Nunca se debe guardar un usuario
        verify(usuarios, never()).save(any(Usuario.class));
    }

    // Marca el método como prueba
    @Test
    // Con credenciales correctas debe devolver el token y los datos públicos
    void iniciaSesionConCredencialesCorrectas() {
        // Usuario existente con su hash
        Usuario usuario = new Usuario("Ana Pérez", "ana@correo.com", "hash-guardado", "Ingeniería", 5);
        // El correo normalizado existe
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        // La contraseña coincide con el hash guardado
        when(cifrador.matches("Clave-123456", "hash-guardado")).thenReturn(true);
        // El servicio de tokens devuelve un token de una hora
        when(jwt.emitir(usuario)).thenReturn(new TokenEmitido("token-de-ejemplo", Instant.now().plusSeconds(3600)));

        // Ejecuta el inicio de sesión
        AuthResponse respuesta = servicio.login(login, IP);

        // El token es el emitido
        assertThat(respuesta.token()).isEqualTo("token-de-ejemplo");
        // El tipo de token es Bearer
        assertThat(respuesta.tipo()).isEqualTo("Bearer");
        // La vigencia es positiva
        assertThat(respuesta.expiraEnSegundos()).isPositive();
        // La respuesta incluye los datos públicos del usuario
        assertThat(respuesta.usuario().correo()).isEqualTo("ana@correo.com");
    }

    // Marca el método como prueba
    @Test
    // Una contraseña incorrecta debe fallar sin emitir token
    void rechazaUnaContrasenaIncorrecta() {
        // Usuario existente con su hash
        Usuario usuario = new Usuario("Ana Pérez", "ana@correo.com", "hash-guardado", "Ingeniería", 5);
        // El correo normalizado existe
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        // La contraseña no coincide
        when(cifrador.matches("Clave-123456", "hash-guardado")).thenReturn(false);

        // Debe fallar con credenciales inválidas
        assertThatThrownBy(() -> servicio.login(login, IP)).isInstanceOf(CredencialesInvalidasException.class);
        // Nunca se debe emitir un token
        verify(jwt, never()).emitir(any(Usuario.class));
    }

    // Marca el método como prueba
    @Test
    // Un correo inexistente debe fallar igual y aun así comparar contra un hash falso
    void rechazaUnCorreoInexistenteConElMismoError() {
        // El correo no existe
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.empty());

        // Debe fallar con el mismo error que una contraseña incorrecta
        assertThatThrownBy(() -> servicio.login(login, IP)).isInstanceOf(CredencialesInvalidasException.class);
        // Aun así se hizo la comparación para que el tiempo de respuesta no delate si el correo existe
        verify(cifrador).matches(any(), any());
        // Nunca se debe emitir un token
        verify(jwt, never()).emitir(any(Usuario.class));
    }

    // Marca el método como prueba
    @Test
    // Una contraseña incorrecta debe anotarse como fallo
    void registraUnFalloCuandoLaContrasenaEsIncorrecta() {
        // Usuario existente con su hash
        Usuario usuario = new Usuario("Ana Pérez", "ana@correo.com", "hash-guardado", "Ingeniería", 5);
        // El correo normalizado existe
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        // La contraseña no coincide
        when(cifrador.matches("Clave-123456", "hash-guardado")).thenReturn(false);

        // Debe fallar con credenciales inválidas
        assertThatThrownBy(() -> servicio.login(login, IP)).isInstanceOf(CredencialesInvalidasException.class);

        // El fallo se anotó con la IP y el correo normalizado
        verify(limite).registrarFallo(IP, "ana@correo.com");
        // No se registró ningún éxito
        verify(limite, never()).registrarExito(any(), any());
    }

    // Marca el método como prueba
    @Test
    // Un inicio de sesión correcto debe reiniciar el contador
    void registraElExitoAlIniciarSesion() {
        // Usuario existente con su hash
        Usuario usuario = new Usuario("Ana Pérez", "ana@correo.com", "hash-guardado", "Ingeniería", 5);
        // El correo normalizado existe
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        // La contraseña coincide
        when(cifrador.matches("Clave-123456", "hash-guardado")).thenReturn(true);
        // El servicio de tokens devuelve un token de una hora
        when(jwt.emitir(usuario)).thenReturn(new TokenEmitido("token-de-ejemplo", Instant.now().plusSeconds(3600)));

        // Inicia sesión
        servicio.login(login, IP);

        // Se reinició el contador de la cuenta
        verify(limite).registrarExito(IP, "ana@correo.com");
        // No se anotó ningún fallo
        verify(limite, never()).registrarFallo(any(), any());
    }

    // Marca el método como prueba
    @Test
    // Si hay bloqueo, no se debe consultar la base de datos ni comparar contraseñas
    void elBloqueoImpideConsultarLaBaseDeDatos() {
        // El limitador rechaza el intento
        doThrow(new DemasiadosIntentosException(60)).when(limite).exigirPermitido(IP, "ana@correo.com");

        // Debe fallar con exceso de intentos
        assertThatThrownBy(() -> servicio.login(login, IP)).isInstanceOf(DemasiadosIntentosException.class);

        // No se consultó a los usuarios
        verifyNoInteractions(usuarios);
        // No se comparó ninguna contraseña
        verify(cifrador, never()).matches(any(), any());
    }
}
