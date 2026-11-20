// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Entidad de la que se toman los datos
import com.orvian.api.entity.Usuario;
// Rol del usuario
import com.orvian.api.entity.Rol;

// Datos públicos de un usuario: nunca incluye la contraseña ni su hash
public record UsuarioResponse(
        // Identificador del usuario
        Long id,
        // Nombre completo
        String nombre,
        // Correo electrónico
        String correo,
        // Rol dentro de la plataforma
        Rol rol,
        // Carrera que estudia
        String carrera,
        // Ciclo académico
        Integer ciclo) {

    // Convierte una entidad en su versión pública
    public static UsuarioResponse desde(Usuario usuario) {
        // Copia solo los campos que se pueden exponer
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(),
                usuario.getRol(), usuario.getCarrera(), usuario.getCiclo());
    }
}
