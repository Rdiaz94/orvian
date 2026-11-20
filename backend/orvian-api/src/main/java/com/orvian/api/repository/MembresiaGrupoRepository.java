// Paquete de los repositorios de la capa de datos
package com.orvian.api.repository;

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

// Entidad que administra este repositorio
import com.orvian.api.entity.MembresiaGrupo;

// Repositorio de membresías: Spring genera la implementación automáticamente
public interface MembresiaGrupoRepository extends JpaRepository<MembresiaGrupo, Long> {

    // Carga el grupo y su curso en la misma consulta para evitar consultas extra por cada fila
    @EntityGraph(attributePaths = {"grupo", "grupo.curso"})
    // Lista paginada de los grupos de un usuario
    Page<MembresiaGrupo> findByUsuarioId(Long usuarioId, Pageable pageable);

    // Carga el usuario junto con cada membresía para evitar una consulta extra por fila
    @EntityGraph(attributePaths = "usuario")
    // Lista de integrantes de un grupo
    List<MembresiaGrupo> findByGrupoId(Long grupoId);

    // Carga el grupo y su curso en la misma consulta
    @EntityGraph(attributePaths = {"grupo", "grupo.curso"})
    // Busca la membresía de un usuario en un grupo concreto
    Optional<MembresiaGrupo> findByGrupoIdAndUsuarioId(Long grupoId, Long usuarioId);

    // Comprueba si un usuario ya pertenece a un grupo
    boolean existsByGrupoIdAndUsuarioId(Long grupoId, Long usuarioId);

    // Borra todas las membresías de un grupo, que se hace antes de borrar el grupo
    void deleteByGrupoId(Long grupoId);

    // Cuenta los grupos a los que pertenece un usuario
    long countByUsuarioId(Long usuarioId);
}
