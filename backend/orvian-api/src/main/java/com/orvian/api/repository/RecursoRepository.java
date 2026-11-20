// Paquete de los repositorios de la capa de datos
package com.orvian.api.repository;

// Colección genérica
import java.util.Collection;
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
// Anotación que indica que la consulta modifica datos
import org.springframework.data.jpa.repository.Modifying;
// Anotación para escribir una consulta JPQL propia
import org.springframework.data.jpa.repository.Query;
// Anotación que enlaza un parámetro con la consulta
import org.springframework.data.repository.query.Param;

// Entidad que administra este repositorio
import com.orvian.api.entity.Recurso;
// Tipos de recurso disponibles
import com.orvian.api.entity.TipoRecurso;

// Repositorio de recursos: toda consulta de lectura aplica la regla de visibilidad
public interface RecursoRepository extends JpaRepository<Recurso, Long> {

    // Carga el autor, el curso y el grupo en la misma consulta para evitar consultas extra por cada fila
    @EntityGraph(attributePaths = {"autor", "curso", "grupo"})
    // Un recurso es visible si es público o si el usuario pertenece a su grupo; los parámetros nunca se concatenan
    @Query("""
            select r from Recurso r
            where (r.grupo is null or r.grupo.id in (select m.grupo.id from MembresiaGrupo m where m.usuario.id = :usuarioId))
              and r.tipo in :tipos
              and lower(r.titulo) like lower(concat('%', :texto, '%'))
            """)
    // Búsqueda paginada de los recursos visibles, con filtro por tipos y por texto en el título
    Page<Recurso> buscarVisibles(@Param("usuarioId") Long usuarioId, @Param("tipos") Collection<TipoRecurso> tipos,
            @Param("texto") String texto, Pageable pageable);

    // Carga el autor, el curso y el grupo en la misma consulta
    @EntityGraph(attributePaths = {"autor", "curso", "grupo"})
    // Aplica la misma regla de visibilidad: un recurso de un grupo ajeno no se puede consultar por su identificador
    @Query("""
            select r from Recurso r
            where r.id = :id
              and (r.grupo is null or r.grupo.id in (select m.grupo.id from MembresiaGrupo m where m.usuario.id = :usuarioId))
            """)
    // Busca un recurso solo si el usuario puede verlo
    Optional<Recurso> buscarVisiblePorId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    // Indica que la consulta cambia datos
    @Modifying
    // Suma una descarga directamente en la base de datos, sin perder conteos si hay accesos simultáneos
    @Query("update Recurso r set r.descargas = r.descargas + 1 where r.id = :id")
    // Devuelve la cantidad de filas actualizadas
    int incrementarDescargas(@Param("id") Long id);

    // Borra los recursos de un grupo al eliminarlo; no se vuelven públicos para no exponer contenido privado
    void deleteByGrupoId(Long grupoId);

    // Cuenta los recursos que compartió un usuario
    long countByAutorId(Long autorId);
}
