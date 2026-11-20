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
// La tabla se llamará recursos y tendrá índices para filtrar por tipo y por grupo
@Table(name = "recursos", indexes = {@Index(name = "idx_recursos_tipo", columnList = "tipo"), @Index(name = "idx_recursos_grupo", columnList = "grupo_id")})
// Representa un material de estudio compartido: guarda su enlace y no el archivo
public class Recurso extends EntidadBase {

    // Obligatorio y de hasta 150 caracteres
    @Column(nullable = false, length = 150)
    // Título del recurso
    private String titulo;

    // Guarda el nombre del tipo como texto
    @Enumerated(EnumType.STRING)
    // Obligatorio y de hasta 20 caracteres
    @Column(nullable = false, length = 20)
    // Tipo de recurso
    private TipoRecurso tipo;

    // Obligatorio y de hasta 500 caracteres
    @Column(nullable = false, length = 500)
    // Enlace donde está alojado el recurso
    private String url;

    // Opcional y de hasta 500 caracteres
    @Column(length = 500)
    // Descripción breve del recurso
    private String descripcion;

    // Muchos recursos pertenecen a un curso y se cargan solo cuando se necesitan
    @ManyToOne(fetch = FetchType.LAZY)
    // La clave foránea es opcional
    @JoinColumn(name = "curso_id")
    // Curso al que se asocia el recurso
    private Curso curso;

    // Muchos recursos pueden pertenecer a un grupo y se cargan solo cuando se necesitan
    @ManyToOne(fetch = FetchType.LAZY)
    // La clave foránea es opcional, porque hay recursos públicos
    @JoinColumn(name = "grupo_id")
    // Grupo con el que se comparte el recurso
    private Grupo grupo;

    // Muchos recursos pertenecen a un autor y se cargan solo cuando se necesitan
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // La clave foránea es obligatoria
    @JoinColumn(name = "autor_id", nullable = false)
    // Usuario que compartió el recurso
    private Usuario autor;

    // Obligatorio; se incrementa con una consulta atómica en el repositorio
    @Column(nullable = false)
    // Cantidad de descargas; empieza en cero
    private int descargas;

    // Hibernate asigna la fecha al insertar el registro
    @CreationTimestamp
    // Obligatoria y no se modifica después de crearse
    @Column(nullable = false, updatable = false)
    // Momento en que se compartió el recurso
    private Instant fechaCreacion;

    // Constructor sin argumentos que exige JPA
    protected Recurso() {
    }

    // Constructor con los datos mínimos de un recurso
    public Recurso(String titulo, TipoRecurso tipo, String url, Usuario autor) {
        // Asigna el título recibido
        this.titulo = titulo;
        // Asigna el tipo recibido
        this.tipo = tipo;
        // Asigna el enlace recibido
        this.url = url;
        // Asigna el autor recibido
        this.autor = autor;
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

    // Devuelve el tipo
    public TipoRecurso getTipo() {
        // Retorna el tipo
        return tipo;
    }

    // Cambia el tipo
    public void setTipo(TipoRecurso tipo) {
        // Actualiza el tipo
        this.tipo = tipo;
    }

    // Devuelve el enlace
    public String getUrl() {
        // Retorna el enlace
        return url;
    }

    // Cambia el enlace
    public void setUrl(String url) {
        // Actualiza el enlace
        this.url = url;
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

    // Devuelve el autor
    public Usuario getAutor() {
        // Retorna el autor
        return autor;
    }

    // Devuelve la cantidad de descargas
    public int getDescargas() {
        // Retorna las descargas
        return descargas;
    }

    // Devuelve la fecha de creación
    public Instant getFechaCreacion() {
        // Retorna la fecha de creación
        return fechaCreacion;
    }
}
