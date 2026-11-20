// Paquete de las entidades de la capa de datos
package com.orvian.api.entity;

// Importa la fecha y hora con precisión de instante
import java.time.Instant;

// Anotación para personalizar una columna
import jakarta.persistence.Column;
// Anotación que marca la clase como tabla de la base de datos
import jakarta.persistence.Entity;
// Modos de carga de las relaciones
import jakarta.persistence.FetchType;
// Anotación que define la columna de la clave foránea
import jakarta.persistence.JoinColumn;
// Anotación para una relación de muchos a uno
import jakarta.persistence.ManyToOne;
// Anotación para definir el nombre de la tabla
import jakarta.persistence.Table;
// Anotación de Hibernate que asigna la fecha al crear el registro
import org.hibernate.annotations.CreationTimestamp;

// Indica que esta clase se guarda como una tabla
@Entity
// La tabla se llamará grupos
@Table(name = "grupos")
// Representa un grupo de trabajo para proyectos académicos
public class Grupo extends EntidadBase {

    // Obligatorio y de hasta 100 caracteres
    @Column(nullable = false, length = 100)
    // Nombre del grupo
    private String nombre;

    // Opcional y de hasta 500 caracteres
    @Column(length = 500)
    // Descripción o propósito del grupo
    private String descripcion;

    // Muchos grupos pueden pertenecer a un curso y se cargan solo cuando se necesitan
    @ManyToOne(fetch = FetchType.LAZY)
    // La clave foránea es opcional
    @JoinColumn(name = "curso_id")
    // Curso al que se asocia el grupo
    private Curso curso;

    // Muchos grupos pueden ser creados por un usuario y se cargan solo cuando se necesitan
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // La clave foránea es obligatoria
    @JoinColumn(name = "creador_id", nullable = false)
    // Usuario que creó el grupo
    private Usuario creador;

    // Hibernate asigna la fecha al insertar el registro
    @CreationTimestamp
    // Obligatoria y no se modifica después de crearse
    @Column(nullable = false, updatable = false)
    // Momento en que se creó el grupo
    private Instant fechaCreacion;

    // Constructor sin argumentos que exige JPA
    protected Grupo() {
    }

    // Constructor con los datos mínimos de un grupo
    public Grupo(String nombre, Usuario creador) {
        // Asigna el nombre recibido
        this.nombre = nombre;
        // Asigna el creador recibido
        this.creador = creador;
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

    // Devuelve la descripción
    public String getDescripcion() {
        // Retorna la descripción
        return descripcion;
    }

    // Cambia la descripción
    public void setDescripcion(String descripcion) {
        // Actualiza la descripción
        this.descripcion = descripcion;
    }

    // Devuelve el curso
    public Curso getCurso() {
        // Retorna el curso
        return curso;
    }

    // Cambia el curso
    public void setCurso(Curso curso) {
        // Actualiza el curso
        this.curso = curso;
    }

    // Devuelve el creador
    public Usuario getCreador() {
        // Retorna el creador
        return creador;
    }

    // Devuelve la fecha de creación
    public Instant getFechaCreacion() {
        // Retorna la fecha de creación
        return fechaCreacion;
    }
}
