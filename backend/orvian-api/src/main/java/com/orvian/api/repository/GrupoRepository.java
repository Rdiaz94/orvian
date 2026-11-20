// Paquete de los repositorios de la capa de datos
package com.orvian.api.repository;

// Interfaz de Spring Data que ya incluye guardar, buscar y borrar
import org.springframework.data.jpa.repository.JpaRepository;

// Entidad que administra este repositorio
import com.orvian.api.entity.Grupo;

// Repositorio de grupos: Spring genera la implementación automáticamente
public interface GrupoRepository extends JpaRepository<Grupo, Long> {
}
