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
// Verifica que un método se llamó
import static org.mockito.Mockito.verify;
// Define el comportamiento de un objeto simulado
import static org.mockito.Mockito.when;

// Contenedor para un valor que puede no existir
import java.util.Optional;

// Anotación que marca un método como prueba
import org.junit.jupiter.api.Test;
// Anotación para ejecutar la prueba con Mockito
import org.junit.jupiter.api.extension.ExtendWith;
// Crea un objeto simulado
import org.mockito.Mock;
// Inyecta los simulados en la clase a probar
import org.mockito.InjectMocks;
// Extensión de Mockito para JUnit 5
import org.mockito.junit.jupiter.MockitoExtension;
// Cifrador de contraseñas
import org.springframework.security.crypto.password.PasswordEncoder;

// Datos para cambiar la contraseña
import com.orvian.api.dto.CambioContrasenaRequest;
// Datos que se pueden editar del perfil
import com.orvian.api.dto.PerfilRequest;
// Datos públicos del usuario
import com.orvian.api.dto.UsuarioResponse;
// Entidad de usuario
import com.orvian.api.entity.Usuario;
// Excepción de credenciales incorrectas
import com.orvian.api.exception.CredencialesInvalidasException;
// Excepción por exceso de intentos
import com.orvian.api.exception.DemasiadosIntentosException;
// Excepción cuando se incumple una regla de negocio
import com.orvian.api.exception.SolicitudInvalidaException;
// Repositorio de usuarios
import com.orvian.api.repository.UsuarioRepository;

// Activa Mockito en esta clase de pruebas
@ExtendWith(MockitoExtension.class)
// Pruebas del servicio de perfil sin base de datos
class PerfilServiceTest {

    // Identificador del usuario en las pruebas
    private static final Long USUARIO = 1L;
    // Dirección IP de las pruebas
    private static final String IP = "127.0.0.1";

    // Repositorio simulado: no toca la base de datos
    @Mock
    private UsuarioRepository usuarios;

    // Cifrador simulado
    @Mock
    private PasswordEncoder cifrador;

    // Limitador de intentos simulado
    @Mock
    private LimiteIntentos limite;

    // Servicio real al que se le inyectan los simulados
    @InjectMocks
    private PerfilService servicio;

    // Usuario de ejemplo con una contraseña cifrada
    private final Usuario usuario = new Usuario("Ana", "ana@correo.com", "hash-viejo", "Ingeniería", 5);

    // Marca el método como prueba
    @Test
    // Debe limpiar los textos y guardar una carrera vacía como nula
    void actualizaLosDatosPersonales() {
        // El usuario existe
        when(usuarios.findById(USUARIO)).thenReturn(Optional.of(usuario));

        // Actualiza con espacios sobrantes y una carrera en blanco
        UsuarioResponse respuesta = servicio.actualizar(USUARIO, new PerfilRequest("  Ana Pérez  ", "   ", 6));

        // El nombre quedó limpio
        assertThat(respuesta.nombre()).isEqualTo("Ana Pérez");
        // La carrera vacía quedó nula
        assertThat(respuesta.carrera()).isNull();
        // El ciclo se actualizó
        assertThat(respuesta.ciclo()).isEqualTo(6);
        // El correo no cambió
        assertThat(usuario.getCorreo()).isEqualTo("ana@correo.com");
    }

    // Marca el método como prueba
    @Test
    // Un token de un usuario que ya no existe no sirve
    void rechazaUnUsuarioInexistente() {
        // El usuario no existe
        when(usuarios.findById(USUARIO)).thenReturn(Optional.empty());

        // Debe fallar con credenciales inválidas
        assertThatThrownBy(() -> servicio.actualizar(USUARIO, new PerfilRequest("Ana", null, null))).isInstanceOf(CredencialesInvalidasException.class);
    }

    // Marca el método como prueba
    @Test
    // Con la contraseña actual correcta debe guardar solo el hash nuevo y reiniciar el contador
    void cambiaLaContrasena() {
        // El usuario existe
        when(usuarios.findById(USUARIO)).thenReturn(Optional.of(usuario));
        // La contraseña actual coincide
        when(cifrador.matches("Actual-123", "hash-viejo")).thenReturn(true);
        // La nueva no coincide con la actual
        when(cifrador.matches("Nueva-12345", "hash-viejo")).thenReturn(false);
        // El cifrador devuelve el hash nuevo
        when(cifrador.encode("Nueva-12345")).thenReturn("hash-nuevo");

        // Cambia la contraseña
        servicio.cambiarContrasena(USUARIO, IP, new CambioContrasenaRequest("Actual-123", "Nueva-12345"));

        // Se guardó el hash nuevo
        assertThat(usuario.getContrasenaHash()).isEqualTo("hash-nuevo");
        // Se reinició el contador de intentos
        verify(limite).registrarExito(IP, "ana@correo.com");
    }

    // Marca el método como prueba
    @Test
    // Con la contraseña actual incorrecta debe fallar, anotar el fallo y no tocar el hash
    void rechazaLaContrasenaActualIncorrecta() {
        // El usuario existe
        when(usuarios.findById(USUARIO)).thenReturn(Optional.of(usuario));
        // La contraseña actual no coincide
        when(cifrador.matches("Incorrecta-1", "hash-viejo")).thenReturn(false);

        // Debe fallar con una solicitud inválida, no con una credencial inválida que cerraría la sesión
        assertThatThrownBy(() -> servicio.cambiarContrasena(USUARIO, IP, new CambioContrasenaRequest("Incorrecta-1", "Nueva-12345"))).isInstanceOf(SolicitudInvalidaException.class);

        // Se anotó el fallo
        verify(limite).registrarFallo(IP, "ana@correo.com");
        // El hash no cambió
        assertThat(usuario.getContrasenaHash()).isEqualTo("hash-viejo");
        // Nunca se cifró una contraseña nueva
        verify(cifrador, never()).encode(any());
    }

    // Marca el método como prueba
    @Test
    // La contraseña nueva no puede ser igual a la actual
    void rechazaUnaContrasenaNuevaIgualALaActual() {
        // El usuario existe
        when(usuarios.findById(USUARIO)).thenReturn(Optional.of(usuario));
        // Tanto la actual como la nueva coinciden con el hash guardado
        when(cifrador.matches("Misma-12345", "hash-viejo")).thenReturn(true);

        // Debe fallar con una solicitud inválida
        assertThatThrownBy(() -> servicio.cambiarContrasena(USUARIO, IP, new CambioContrasenaRequest("Misma-12345", "Misma-12345"))).isInstanceOf(SolicitudInvalidaException.class);

        // El hash no cambió
        assertThat(usuario.getContrasenaHash()).isEqualTo("hash-viejo");
    }

    // Marca el método como prueba
    @Test
    // Si hay bloqueo por exceso de intentos, no se compara ninguna contraseña
    void elBloqueoImpideCompararContrasenas() {
        // El usuario existe
        when(usuarios.findById(USUARIO)).thenReturn(Optional.of(usuario));
        // El limitador rechaza el intento
        doThrow(new DemasiadosIntentosException(60)).when(limite).exigirPermitido(IP, "ana@correo.com");

        // Debe fallar con exceso de intentos
        assertThatThrownBy(() -> servicio.cambiarContrasena(USUARIO, IP, new CambioContrasenaRequest("Actual-123", "Nueva-12345"))).isInstanceOf(DemasiadosIntentosException.class);

        // No se comparó ninguna contraseña
        verify(cifrador, never()).matches(any(), any());
    }
}
