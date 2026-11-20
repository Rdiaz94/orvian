// Paquete de los repositorios de la capa de datos
package com.orvian.api.repository;

// Importa la fecha y hora con precisión de instante
import java.time.Instant;
// Lista de resultados
import java.util.List;
// Contenedor para un valor que puede no existir
import java.util.Optional;

// Resultado paginado de una consulta
import org.springframework.data.domain.Page;
// Datos de la página solicitada: número, tamaño y orden
import org.springframework.data.domain.Pageable;
// Anotación que indica qué relaciones cargar en la misma consulta
import org.springframework.data.jpa.repository.EntityGraph;
// Interfaz de Spring Data que ya incluye guardar, buscar y borrar
import org.springframework.data.jpa.repository.JpaRepository;
// Anotación para escribir una consulta JPQL propia
import org.springframework.data.jpa.repository.Query;
// Anotación que enlaza un parámetro con la consulta
import org.springframework.data.repository.query.Param;

// Estado de la tarea
import com.orvian.api.entity.EstadoTarea;
// Entidad de tareas
import com.orvian.api.entity.Tarea;

// Repositorio de tareas: toda consulta incluye al dueño o al grupo para impedir el acceso a datos ajenos
public interface TareaRepository extends JpaRepository<Tarea, Long> {

    // Carga el curso y el grupo en la misma consulta para evitar consultas extra por cada fila
    @EntityGraph(attributePaths = {"curso", "grupo"})
    // Lista paginada de todas las tareas asignadas a un usuario, personales o de grupo
    Page<Tarea> findByResponsableId(Long responsableId, Pageable pageable);

    // Carga el curso y el grupo en la misma consulta
    @EntityGraph(attributePaths = {"curso", "grupo"})
    // Lista paginada de las tareas de un usuario según su estado
    Page<Tarea> findByResponsableIdAndEstado(Long responsableId, EstadoTarea estado, Pageable pageable);

    // Carga el curso y el grupo en la misma consulta
    @EntityGraph(attributePaths = {"curso", "grupo"})
    // Busca una tarea solo si está asignada al usuario indicado
    Optional<Tarea> findByIdAndResponsableId(Long id, Long responsableId);

    // Carga el curso, el grupo y el responsable porque la lista del grupo muestra a cada responsable
    @EntityGraph(attributePaths = {"curso", "grupo", "responsable"})
    // Lista paginada de las tareas de un grupo
    Page<Tarea> findByGrupoId(Long grupoId, Pageable pageable);

    // Carga el curso, el grupo y el responsable en la misma consulta
    @EntityGraph(attributePaths = {"curso", "grupo", "responsable"})
    // Lista paginada de las tareas de un grupo según su estado
    Page<Tarea> findByGrupoIdAndEstado(Long grupoId, EstadoTarea estado, Pageable pageable);

    // Carga el curso, el grupo y el responsable en la misma consulta
    @EntityGraph(attributePaths = {"curso", "grupo", "responsable"})
    // Busca una tarea solo si pertenece al grupo indicado; así un identificador de otro grupo no sirve
    Optional<Tarea> findByIdAndGrupoId(Long id, Long grupoId);

    // Borra todas las tareas de un grupo, que se hace al eliminar el grupo
    void deleteByGrupoId(Long grupoId);

    // Resultado de contar tareas agrupadas por estado
    interface ConteoPorEstado {
        // Estado de las tareas contadas
        EstadoTarea getEstado();

        // Cantidad de tareas en ese estado
        long getTotal();
    }

    // Una sola consulta agrupada reemplaza tres consultas de conteo
    @Query("select t.estado as estado, count(t) as total from Tarea t where t.responsable.id = :usuarioId group by t.estado")
    // Cuenta las tareas asignadas a un usuario según su estado
    List<ConteoPorEstado> contarPorEstado(@Param("usuarioId") Long usuarioId);

    // Busca la tarea más próxima a vencer que no esté en el estado indicado y cuya fecha límite no haya pasado
    Optional<Tarea> findFirstByResponsableIdAndEstadoNotAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAsc(Long responsableId, EstadoTarea estado, Instant desde);
}
