// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Entidad de curso
import com.orvian.api.entity.Curso;
// Entidad de grupo
import com.orvian.api.entity.Grupo;
// Papel dentro del grupo
import com.orvian.api.entity.RolGrupo;

// Datos públicos de un grupo desde el punto de vista de un integrante
public record GrupoResponse(
        // Identificador del grupo
        Long id,
        // Nombre del grupo
        String nombre,
        // Descripción del grupo
        String descripcion,
        // Identificador del curso, si tiene
        Long cursoId,
        // Nombre del curso, si tiene
        String cursoNombre,
        // Papel que tiene en el grupo quien consulta
        RolGrupo miRol) {

    // Convierte una entidad en su versión pública
    public static GrupoResponse desde(Grupo grupo, RolGrupo miRol) {
        // Curso asociado, que puede no existir
        Curso curso = grupo.getCurso();
        // Copia solo los campos que se pueden exponer
        return new GrupoResponse(grupo.getId(), grupo.getNombre(), grupo.getDescripcion(),
                curso == null ? null : curso.getId(), curso == null ? null : curso.getNombre(), miRol);
    }
}
