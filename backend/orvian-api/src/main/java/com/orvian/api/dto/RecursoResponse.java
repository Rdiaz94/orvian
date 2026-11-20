// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Importa la fecha y hora con precisión de instante
import java.time.Instant;

// Entidad de curso
import com.orvian.api.entity.Curso;
// Entidad de grupo
import com.orvian.api.entity.Grupo;
// Entidad de recurso
import com.orvian.api.entity.Recurso;
// Tipo de recurso
import com.orvian.api.entity.TipoRecurso;

// Datos públicos de un recurso; no incluye el correo del autor
public record RecursoResponse(
        // Identificador del recurso
        Long id,
        // Título del recurso
        String titulo,
        // Tipo de recurso
        TipoRecurso tipo,
        // Enlace del recurso
        String url,
        // Descripción del recurso
        String descripcion,
        // Identificador del curso, si tiene
        Long cursoId,
        // Nombre del curso, si tiene
        String cursoNombre,
        // Identificador del grupo, si es un recurso de grupo
        Long grupoId,
        // Nombre del grupo, si es un recurso de grupo
        String grupoNombre,
        // Identificador del autor
        Long autorId,
        // Nombre del autor
        String autorNombre,
        // Cantidad de descargas
        int descargas,
        // Momento en que se compartió
        Instant fechaCreacion,
        // Indica si quien consulta es el autor, para que la pantalla muestre editar y borrar
        boolean esAutor) {

    // Convierte una entidad en su versión pública desde el punto de vista de quien consulta
    public static RecursoResponse desde(Recurso recurso, Long usuarioId) {
        // Curso asociado, que puede no existir
        Curso curso = recurso.getCurso();
        // Grupo asociado, que existe solo en los recursos de grupo
        Grupo grupo = recurso.getGrupo();
        // Copia solo los campos que se pueden exponer
        return new RecursoResponse(recurso.getId(), recurso.getTitulo(), recurso.getTipo(), recurso.getUrl(), recurso.getDescripcion(),
                curso == null ? null : curso.getId(), curso == null ? null : curso.getNombre(),
                grupo == null ? null : grupo.getId(), grupo == null ? null : grupo.getNombre(),
                recurso.getAutor().getId(), recurso.getAutor().getNombre(),
                recurso.getDescargas(), recurso.getFechaCreacion(), recurso.getAutor().getId().equals(usuarioId));
    }
}
