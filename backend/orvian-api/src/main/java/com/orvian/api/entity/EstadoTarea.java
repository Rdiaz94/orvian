// Paquete de las entidades de la capa de datos
package com.orvian.api.entity;

// Estados por los que pasa una tarea, que forman las columnas del tablero
public enum EstadoTarea {
    // La tarea aún no se empezó
    PENDIENTE,
    // La tarea se está trabajando
    EN_PROGRESO,
    // La tarea ya se terminó
    HECHA
}
