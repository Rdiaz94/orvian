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
// Anotación para impedir combinaciones repetidas de columnas
import jakarta.persistence.UniqueConstraint;
// Anotación de Hibernate que asigna la fecha al crear el registro
import org.hibernate.annotations.CreationTimestamp;

// Indica que esta clase se guarda como una tabla
@Entity
// Tabla puente con una restricción que impide repetir al mismo usuario en un grupo y un índice por usuario
@Table(name = "membresias_grupo",
        uniqueConstraints = @UniqueConstraint(name = "uk_membresia_grupo_usuario", columnNames = {"grupo_id", "usuario_id"}),
        indexes = @Index(name = "idx_membresia_usuario", columnList = "usuario_id"))
// Representa la pertenencia de un usuario a un grupo; resuelve la relación de muchos a muchos
public class MembresiaGrupo extends EntidadBase {

    // Muchas membresías pertenecen a un grupo y se cargan solo cuando se necesitan
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // La clave foránea es obligatoria
    @JoinColumn(name = "grupo_id", nullable = false)
    // Grupo al que pertenece la membresía
    private Grupo grupo;

    // Muchas membresías pertenecen a un usuario y se cargan solo cuando se necesitan
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // La clave foránea es obligatoria
    @JoinColumn(name = "usuario_id", nullable = false)
    // Usuario que es integrante del grupo
    private Usuario usuario;

    // Guarda el nombre del papel como texto
    @Enumerated(EnumType.STRING)
    // Obligatorio y de hasta 20 caracteres
    @Column(nullable = false, length = 20)
    // Papel del usuario dentro del grupo; por defecto es miembro
    private RolGrupo rolGrupo = RolGrupo.MIEMBRO;

    // Hibernate asigna la fecha al insertar el registro
    @CreationTimestamp
    // Obligatoria y no se modifica después de crearse
    @Column(nullable = false, updatable = false)
    // Momento en que el usuario ingresó al grupo
    private Instant fechaIngreso;

    // Constructor sin argumentos que exige JPA
    protected MembresiaGrupo() {
    }

    // Constructor con los datos de una membresía
    public MembresiaGrupo(Grupo grupo, Usuario usuario, RolGrupo rolGrupo) {
        // Asigna el grupo recibido
        this.grupo = grupo;
        // Asigna el usuario recibido
        this.usuario = usuario;
        // Asigna el papel recibido
        this.rolGrupo = rolGrupo;
    }

    // Devuelve el grupo
    public Grupo getGrupo() {
        // Retorna el grupo
        return grupo;
    }

    // Devuelve el usuario
    public Usuario getUsuario() {
        // Retorna el usuario
        return usuario;
    }

    // Devuelve el papel dentro del grupo
    public RolGrupo getRolGrupo() {
        // Retorna el papel
        return rolGrupo;
    }

    // Cambia el papel dentro del grupo
    public void setRolGrupo(RolGrupo rolGrupo) {
        // Actualiza el papel
        this.rolGrupo = rolGrupo;
    }

    // Devuelve la fecha de ingreso
    public Instant getFechaIngreso() {
        // Retorna la fecha de ingreso
        return fechaIngreso;
    }
}
