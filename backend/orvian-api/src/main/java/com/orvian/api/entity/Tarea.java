// Paquete de las entidades de la capa de datos
package com.orvian.api.entity;

// Importa la fecha y hora con precisión de instante
import java.time.Instant;

// Anotación para personalizar una columna
import jakarta.persistence.Column;
// Anotación que marca la clase como tabla de la base de datos
import jakarta.persistence.Entity;
// Tipos de almacenamiento para enumeraciones
import jakarta.persistence.EnumType;
// Anotación para guardar una enumeración
import jakarta.persistence.Enumerated;
// Modos de carga de las relaciones
import jakarta.persistence.FetchType;
// Anotación para crear un índice en la tabla
import jakarta.persistence.Index;
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
// La tabla se llamará tareas y tendrá índices para acelerar la lista de cada usuario y la de cada grupo
@Table(name = "tareas", indexes = {@Index(name = "idx_tareas_responsable_estado", columnList = "responsable_id, estado"), @Index(name = "idx_tareas_grupo", columnList = "grupo_id")})
// Representa una tarea de estudio asignada a un usuario
public class Tarea extends EntidadBase {

    // Obligatorio y de hasta 150 caracteres
    @Column(nullable = false, length = 150)
    // Título de la tarea
    private String titulo;

    // Opcional y de hasta 1000 caracteres
    @Column(length = 1000)
    // Descripción de la tarea
    private String descripcion;

    // Muchas tareas pertenecen a un curso y se cargan solo cuando se necesitan
    @ManyToOne(fetch = FetchType.LAZY)
    // La clave foránea es opcional, así que se admiten tareas sin curso
    @JoinColumn(name = "curso_id")
    // Curso al que pertenece la tarea
    private Curso curso;

    // Muchas tareas pueden pertenecer a un grupo y se cargan solo cuando se necesitan
    @ManyToOne(fetch = FetchType.LAZY)
    // La clave foránea es opcional, porque existen tareas personales
    @JoinColumn(name = "grupo_id")
    // Grupo al que pertenece la tarea
    private Grupo grupo;

    // Muchas tareas pertenecen a un usuario y se cargan solo cuando se necesitan
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // La clave foránea es obligatoria
    @JoinColumn(name = "responsable_id", nullable = false)
    // Usuario responsable de la tarea
    private Usuario responsable;

    // Guarda el nombre del estado como texto
    @Enumerated(EnumType.STRING)
    // Obligatorio y de hasta 20 caracteres
    @Column(nullable = false, length = 20)
    // Estado actual; empieza como pendiente
    private EstadoTarea estado = EstadoTarea.PENDIENTE;

    // Guarda el nombre de la etiqueta como texto
    @Enumerated(EnumType.STRING)
    // Obligatorio y de hasta 20 caracteres
    @Column(nullable = false, length = 20)
    // Tipo de tarea; por defecto es otro
    private EtiquetaTarea etiqueta = EtiquetaTarea.OTRO;

    // Momento límite de entrega (opcional)
    private Instant fechaVencimiento;

    // Obligatorio: un número entre 0 y 100 que se valida en la capa de servicio
    @Column(nullable = false)
    // Porcentaje de avance; empieza en cero
    private int progreso;

    // Hibernate asigna la fecha al insertar el registro
    @CreationTimestamp
    // Obligatoria y no se modifica después de crearse
    @Column(nullable = false, updatable = false)
    // Momento en que se creó la tarea
    private Instant fechaCreacion;

    // Constructor sin argumentos que exige JPA
    protected Tarea() {
    }

    // Constructor con los datos mínimos de una tarea
    public Tarea(String titulo, Usuario responsable) {
        // Asigna el título recibido
        this.titulo = titulo;
        // Asigna el responsable recibido
        this.responsable = responsable;
    }

    // Devuelve el título
    public String getTitulo() {
        // Retorna el título
        return titulo;
    }

    // Cambia el título
    public void setTitulo(String titulo) {
        // Actualiza el título
        this.titulo = titulo;
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

    // Devuelve el grupo
    public Grupo getGrupo() {
        // Retorna el grupo
        return grupo;
    }

    // Cambia el grupo
    public void setGrupo(Grupo grupo) {
        // Actualiza el grupo
        this.grupo = grupo;
    }

    // Devuelve el responsable
    public Usuario getResponsable() {
        // Retorna el responsable
        return responsable;
    }

    // Cambia el responsable; solo lo usa el líder al reasignar una tarea de grupo
    public void setResponsable(Usuario responsable) {
        // Actualiza el responsable
        this.responsable = responsable;
    }

    // Devuelve el estado
    public EstadoTarea getEstado() {
        // Retorna el estado
        return estado;
    }

    // Cambia el estado
    public void setEstado(EstadoTarea estado) {
        // Actualiza el estado
        this.estado = estado;
    }

    // Devuelve la etiqueta
    public EtiquetaTarea getEtiqueta() {
        // Retorna la etiqueta
        return etiqueta;
    }

    // Cambia la etiqueta
    public void setEtiqueta(EtiquetaTarea etiqueta) {
        // Actualiza la etiqueta
        this.etiqueta = etiqueta;
    }

    // Devuelve la fecha de vencimiento
    public Instant getFechaVencimiento() {
        // Retorna la fecha de vencimiento
        return fechaVencimiento;
    }

    // Cambia la fecha de vencimiento
    public void setFechaVencimiento(Instant fechaVencimiento) {
        // Actualiza la fecha de vencimiento
        this.fechaVencimiento = fechaVencimiento;
    }

    // Devuelve el progreso
    public int getProgreso() {
        // Retorna el porcentaje de avance
        return progreso;
    }

    // Cambia el progreso
    public void setProgreso(int progreso) {
        // Actualiza el porcentaje de avance
        this.progreso = progreso;
    }

    // Devuelve la fecha de creación
    public Instant getFechaCreacion() {
        // Retorna la fecha de creación
        return fechaCreacion;
    }
}
