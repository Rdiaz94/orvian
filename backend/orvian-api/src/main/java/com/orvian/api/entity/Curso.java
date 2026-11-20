// Paquete de las entidades de la capa de datos
package com.orvian.api.entity;

// Anotación para personalizar una columna
import jakarta.persistence.Column;
// Anotación que marca la clase como tabla de la base de datos
import jakarta.persistence.Entity;
// Anotación para definir el nombre de la tabla
import jakarta.persistence.Table;

// Indica que esta clase se guarda como una tabla
@Entity
// La tabla se llamará cursos
@Table(name = "cursos")
// Representa un curso al que pertenecen tareas, grupos y recursos
public class Curso extends EntidadBase {

    // Obligatorio, único y de hasta 100 caracteres
    @Column(nullable = false, unique = true, length = 100)
    // Nombre del curso
    private String nombre;

    // Constructor sin argumentos que exige JPA
    protected Curso() {
    }

    // Constructor para crear cursos nuevos desde el código
    public Curso(String nombre) {
        // Asigna el nombre recibido
        this.nombre = nombre;
    }

    // Devuelve el nombre
    public String getNombre() {
        // Retorna el nombre
        return nombre;
    }

    // Cambia el nombre
    public void setNombre(String nombre) {
        // Actualiza el nombre
        this.nombre = nombre;
    }
}
