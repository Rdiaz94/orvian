// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Importa la fecha y hora con precisión de instante
import java.time.Instant;

// Papel dentro del grupo
import com.orvian.api.entity.RolGrupo;
// Entidad de membresía
import com.orvian.api.entity.MembresiaGrupo;
// Entidad de usuario
import com.orvian.api.entity.Usuario;

// Datos públicos de un integrante: no incluye su correo para proteger su privacidad
public record IntegranteResponse(
        // Identificador del usuario
        Long usuarioId,
        // Nombre del integrante
        String nombre,
        // Carrera del integrante
        String carrera,
        // Ciclo del integrante
        Integer ciclo,
        // Papel dentro del grupo
        RolGrupo rolGrupo,
        // Momento en que ingresó al grupo
        Instant fechaIngreso) {

    // Convierte una membresía en su versión pública
    public static IntegranteResponse desde(MembresiaGrupo membresia) {
        // Usuario que es integrante
        Usuario usuario = membresia.getUsuario();
        // Copia solo los campos que se pueden exponer
        return new IntegranteResponse(usuario.getId(), usuario.getNombre(), usuario.getCarrera(), usuario.getCiclo(),
                membresia.getRolGrupo(), membresia.getFechaIngreso());
    }
}
