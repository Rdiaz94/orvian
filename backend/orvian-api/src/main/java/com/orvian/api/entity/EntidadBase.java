// Paquete de las entidades de la capa de datos
package com.orvian.api.entity;

// Anotación que marca la clave primaria
import jakarta.persistence.Id;
// Anotación para generar el identificador automáticamente
import jakarta.persistence.GeneratedValue;
// Estrategias de generación del identificador
import jakarta.persistence.GenerationType;
// Anotación que comparte los campos con las entidades hijas sin crear tabla propia
import jakarta.persistence.MappedSuperclass;
// Utilidad de Hibernate para obtener la clase real de un objeto, incluso si es un proxy
import org.hibernate.Hibernate;

// Sus campos se heredan en cada tabla de las entidades hijas
@MappedSuperclass
// Clase base abstracta: concentra el identificador y la comparación de todas las entidades
public abstract class EntidadBase {

    // Clave primaria de la tabla
    @Id
    // La base de datos genera el valor con una columna autoincremental
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Identificador único de la entidad
    private Long id;

    // Devuelve el identificador
    public Long getId() {
        // Retorna el valor del identificador
        return id;
    }

    // Dos entidades son iguales si son de la misma clase y tienen el mismo identificador
    @Override
    // Método de comparación que usan JPA y las colecciones
    public boolean equals(Object otro) {
        // Si es el mismo objeto, son iguales
        if (this == otro) {
            // Retorna verdadero
            return true;
        }
        // Si es nulo o de otra clase real, no son iguales
        if (otro == null || Hibernate.getClass(this) != Hibernate.getClass(otro)) {
            // Retorna falso
            return false;
        }
        // Compara los identificadores, ignorando entidades sin guardar
        return id != null && id.equals(((EntidadBase) otro).getId());
    }

    // Valor constante para que el hash no cambie al guardar la entidad
    @Override
    // Método que complementa a equals
    public int hashCode() {
        // Retorna el hash de la clase real
        return Hibernate.getClass(this).hashCode();
    }
}
