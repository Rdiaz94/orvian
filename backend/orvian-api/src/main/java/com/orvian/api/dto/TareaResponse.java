// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Importa la fecha y hora con precisión de instante
import java.time.Instant;

// Entidad de curso
import com.orvian.api.entity.Curso;
// Estado de la tarea
import com.orvian.api.entity.EstadoTarea;
// Etiqueta de la tarea
import com.orvian.api.entity.EtiquetaTarea;
// Entidad de grupo
import com.orvian.api.entity.Grupo;
// Entidad de tarea
import com.orvian.api.entity.Tarea;
// Entidad de usuario
import com.orvian.api.entity.Usuario;

// Datos públicos de una tarea; no incluye el correo del responsable
public record TareaResponse(
        // Identificador de la tarea
        Long id,
        // Título de la tarea
        String titulo,
        // Descripción de la tarea
        String descripcion,
        // Identificador del curso, si tiene
        Long cursoId,
        // Nombre del curso, si tiene
        String cursoNombre,
        // Identificador del grupo, si es una tarea de grupo
        Long grupoId,
        // Nombre del grupo, si es una tarea de grupo
        String grupoNombre,
        // Identificador de la persona responsable
        Long responsableId,
        // Nombre de la persona responsable
        String responsableNombre,
        // Estado actual
        EstadoTarea estado,
        // Etiqueta de la tarea
        EtiquetaTarea etiqueta,
        // Fecha límite de entrega
        Instant fechaVencimiento,
        // Porcentaje de avance
        int progreso,
        // Momento de creación
        Instant fechaCreacion) {

    // Convierte una entidad en su versión pública
    public static TareaResponse desde(Tarea tarea) {
        // Curso asociado, que puede no existir
        Curso curso = tarea.getCurso();
        // Grupo asociado, que existe solo en las tareas de grupo
        Grupo grupo = tarea.getGrupo();
        // Responsable de la tarea, que siempre existe
        Usuario responsable = tarea.getResponsable();
        // Copia solo los campos que se pueden exponer
        return new TareaResponse(tarea.getId(), tarea.getTitulo(), tarea.getDescripcion(),
                curso == null ? null : curso.getId(), curso == null ? null : curso.getNombre(),
                grupo == null ? null : grupo.getId(), grupo == null ? null : grupo.getNombre(),
                responsable.getId(), responsable.getNombre(),
                tarea.getEstado(), tarea.getEtiqueta(), tarea.getFechaVencimiento(), tarea.getProgreso(), tarea.getFechaCreacion());
    }
}
