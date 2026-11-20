// Paquete de los repositorios de la capa de datos
package com.orvian.api.repository;

// Contenedor para un valor que puede no existir
import java.util.Optional;

// Interfaz de Spring Data que ya incluye guardar, buscar y borrar
import org.springframework.data.jpa.repository.JpaRepository;

// Entidad que administra este repositorio
import com.orvian.api.entity.Usuario;

// Repositorio de usuarios: Spring genera la implementación automáticamente
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca un usuario por su correo, que es único
    Optional<Usuario> findByCorreo(String correo);

    // Comprueba si ya existe un usuario con ese correo
    boolean existsByCorreo(String correo);
}
