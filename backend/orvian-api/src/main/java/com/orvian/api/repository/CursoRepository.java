// Paquete de los repositorios de la capa de datos
package com.orvian.api.repository;

// Lista de resultados
import java.util.List;

// Interfaz de Spring Data que ya incluye guardar, buscar y borrar
import org.springframework.data.jpa.repository.JpaRepository;

// Entidad que administra este repositorio
import com.orvian.api.entity.Curso;

// Repositorio de cursos: Spring genera la implementación automáticamente
public interface CursoRepository extends JpaRepository<Curso, Long> {

    // Comprueba si ya existe un curso con ese nombre, sin distinguir mayúsculas
    boolean existsByNombreIgnoreCase(String nombre);

    // Lista todos los cursos ordenados alfabéticamente
    List<Curso> findAllByOrderByNombreAsc();
}
