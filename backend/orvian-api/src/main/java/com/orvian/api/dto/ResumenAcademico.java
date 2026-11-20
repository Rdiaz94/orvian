// Paquete de los objetos que viajan entre el cliente y la API
package com.orvian.api.dto;

// Importa la fecha y hora con precisión de instante
import java.time.Instant;

// Resumen de la actividad académica de un usuario; lo consume el frontend en Inicio y Perfil
public record ResumenAcademico(
        // Cantidad total de tareas asignadas
        int tareasTotales,
        // Tareas que aún no se empiezan
        int tareasPendientes,
        // Tareas que se están trabajando
        int tareasEnProgreso,
        // Tareas terminadas
        int tareasHechas,
        // Porcentaje de tareas terminadas sobre el total
        int porcentajeCumplimiento,
        // Grupos a los que pertenece
        int gruposActivos,
        // Recursos que compartió
        int recursosCompartidos,
        // Próxima tarea por vencer, o nulo si no hay ninguna
        ProximaEntrega proximaEntrega) {

    // Datos de la próxima entrega
    public record ProximaEntrega(
            // Título de la tarea
            String titulo,
            // Fecha y hora límite
            Instant fechaVencimiento) {
    }
}
