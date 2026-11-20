// Paquete de la capa de lógica de negocio
package com.orvian.api.service;

// Cifrador de contraseñas
import org.springframework.security.crypto.password.PasswordEncoder;
// Anotación que marca la clase como servicio
import org.springframework.stereotype.Service;
// Anotación que ejecuta el método dentro de una transacción
import org.springframework.transaction.annotation.Transactional;

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
// Excepción cuando se incumple una regla de negocio
import com.orvian.api.exception.SolicitudInvalidaException;
// Repositorio de usuarios
import com.orvian.api.repository.UsuarioRepository;

// Lógica de negocio del perfil: cada usuario solo puede modificar su propia cuenta, que sale del token
@Service
// Clase que edita los datos personales y cambia la contraseña
public class PerfilService {

    // Acceso a los datos de usuarios
    private final UsuarioRepository usuarios;
    // Cifrador de contraseñas
    private final PasswordEncoder cifrador;
    // Frena los intentos de fuerza bruta contra la contraseña actual
    private final LimiteIntentos limite;

    // Spring entrega las dependencias por el constructor
    public PerfilService(UsuarioRepository usuarios, PasswordEncoder cifrador, LimiteIntentos limite) {
        // Guarda el repositorio
        this.usuarios = usuarios;
        // Guarda el cifrador
        this.cifrador = cifrador;
        // Guarda el limitador de intentos
        this.limite = limite;
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Actualiza el nombre, la carrera y el ciclo del usuario
    public UsuarioResponse actualizar(Long usuarioId, PerfilRequest datos) {
        // Busca al usuario del token
        Usuario usuario = buscar(usuarioId);
        // Guarda el nombre sin espacios sobrantes
        usuario.setNombre(datos.nombre().trim());
        // Una carrera vacía se guarda como nula
        usuario.setCarrera(datos.carrera() == null || datos.carrera().isBlank() ? null : datos.carrera().trim());
        // Guarda el ciclo
        usuario.setCiclo(datos.ciclo());
        // Los cambios se guardan solos al terminar la transacción
        return UsuarioResponse.desde(usuario);
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Cambia la contraseña si la actual es correcta
    public void cambiarContrasena(Long usuarioId, String ip, CambioContrasenaRequest datos) {
        // Busca al usuario del token
        Usuario usuario = buscar(usuarioId);
        // Si ya se superó el límite de intentos, se rechaza antes de comparar nada
        limite.exigirPermitido(ip, usuario.getCorreo());
        // La contraseña actual debe coincidir; se responde 400 y no 401 porque el token sí es válido
        if (!cifrador.matches(datos.contrasenaActual(), usuario.getContrasenaHash())) {
            // Anota el fallo para el límite de intentos
            limite.registrarFallo(ip, usuario.getCorreo());
            // Rechaza la petición con el motivo
            throw new SolicitudInvalidaException("La contraseña actual no es correcta");
        }
        // La nueva debe ser distinta de la actual
        if (cifrador.matches(datos.contrasenaNueva(), usuario.getContrasenaHash())) {
            // Rechaza la petición con el motivo
            throw new SolicitudInvalidaException("La contraseña nueva debe ser distinta de la actual");
        }
        // Guarda solo el hash de la contraseña nueva
        usuario.setContrasenaHash(cifrador.encode(datos.contrasenaNueva()));
        // Demostró conocer la contraseña actual, así que se reinicia el contador
        limite.registrarExito(ip, usuario.getCorreo());
    }

    // Busca al usuario; si ya no existe, el token deja de ser útil
    private Usuario buscar(Long usuarioId) {
        // Consulta por identificador
        return usuarios.findById(usuarioId).orElseThrow(CredencialesInvalidasException::new);
    }
}
