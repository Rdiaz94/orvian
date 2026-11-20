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
// Anotación para definir el nombre de la tabla
import jakarta.persistence.Table;
// Anotación de Hibernate que asigna la fecha al crear el registro
import org.hibernate.annotations.CreationTimestamp;

// Indica que esta clase se guarda como una tabla
@Entity
// La tabla se llamará usuarios
@Table(name = "usuarios")
// Representa a una persona que usa Orvian; hereda el identificador de EntidadBase
public class Usuario extends EntidadBase {

    // La columna es obligatoria y admite hasta 100 caracteres
    @Column(nullable = false, length = 100)
    // Nombre completo del usuario
    private String nombre;

    // Obligatorio, único y de hasta 150 caracteres
    @Column(nullable = false, unique = true, length = 150)
    // Correo electrónico usado para iniciar sesión
    private String correo;

    // Obligatorio y con espacio suficiente para un hash BCrypt
    @Column(nullable = false, length = 100)
    // Contraseña cifrada: nunca se guarda en texto plano
    private String contrasenaHash;

    // Guarda el nombre del rol como texto y no como número
    @Enumerated(EnumType.STRING)
    // Obligatorio y de hasta 20 caracteres
    @Column(nullable = false, length = 20)
    // Rol del usuario; por defecto es estudiante
    private Rol rol = Rol.ESTUDIANTE;

    // Opcional y de hasta 100 caracteres
    @Column(length = 100)
    // Carrera que estudia el usuario
    private String carrera;

    // Ciclo académico actual (opcional)
    private Integer ciclo;

    // Hibernate asigna la fecha al insertar el registro
    @CreationTimestamp
    // Obligatoria y no se modifica después de crearse
    @Column(nullable = false, updatable = false)
    // Momento en que el usuario se registró
    private Instant fechaRegistro;

    // Constructor sin argumentos que exige JPA
    protected Usuario() {
    }

    // Constructor para crear usuarios nuevos desde el código
    public Usuario(String nombre, String correo, String contrasenaHash, String carrera, Integer ciclo) {
        // Asigna el nombre recibido
        this.nombre = nombre;
        // Asigna el correo recibido
        this.correo = correo;
        // Asigna la contraseña ya cifrada
        this.contrasenaHash = contrasenaHash;
        // Asigna la carrera recibida
        this.carrera = carrera;
        // Asigna el ciclo recibido
        this.ciclo = ciclo;
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

    // Devuelve el correo
    public String getCorreo() {
        // Retorna el correo
        return correo;
    }

    // Cambia el correo
    public void setCorreo(String correo) {
        // Actualiza el correo
        this.correo = correo;
    }

    // Devuelve la contraseña cifrada
    public String getContrasenaHash() {
        // Retorna el hash de la contraseña
        return contrasenaHash;
    }

    // Cambia la contraseña cifrada
    public void setContrasenaHash(String contrasenaHash) {
        // Actualiza el hash de la contraseña
        this.contrasenaHash = contrasenaHash;
    }

    // Devuelve el rol
    public Rol getRol() {
        // Retorna el rol
        return rol;
    }

    // Cambia el rol
    public void setRol(Rol rol) {
        // Actualiza el rol
        this.rol = rol;
    }

    // Devuelve la carrera
    public String getCarrera() {
        // Retorna la carrera
        return carrera;
    }

    // Cambia la carrera
    public void setCarrera(String carrera) {
        // Actualiza la carrera
        this.carrera = carrera;
    }

    // Devuelve el ciclo
    public Integer getCiclo() {
        // Retorna el ciclo
        return ciclo;
    }

    // Cambia el ciclo
    public void setCiclo(Integer ciclo) {
        // Actualiza el ciclo
        this.ciclo = ciclo;
    }

    // Devuelve la fecha de registro
    public Instant getFechaRegistro() {
        // Retorna la fecha de registro
        return fechaRegistro;
    }
}
