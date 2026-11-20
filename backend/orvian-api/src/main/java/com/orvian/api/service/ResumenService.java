// Paquete de la capa de lógica de negocio
package com.orvian.api.service;

// Reloj que se inyecta para poder probar las fechas
import java.time.Clock;
// Importa la fecha y hora con precisión de instante
import java.time.Instant;
// Mapa optimizado para enumeraciones
import java.util.EnumMap;
// Interfaz de mapas
import java.util.Map;

// Anotación que marca la clase como servicio
import org.springframework.stereotype.Service;
// Anotación que ejecuta el método dentro de una transacción
import org.springframework.transaction.annotation.Transactional;

// Resumen de la actividad académica
import com.orvian.api.dto.ResumenAcademico;
// Estado de la tarea
import com.orvian.api.entity.EstadoTarea;
// Repositorio de membresías
import com.orvian.api.repository.MembresiaGrupoRepository;
// Repositorio de recursos
import com.orvian.api.repository.RecursoRepository;
// Repositorio de tareas
import com.orvian.api.repository.TareaRepository;

// Lógica de negocio del resumen académico: calcula las estadísticas de un usuario
@Service
// Clase que calcula las estadísticas de un usuario
public class ResumenService {

    // Acceso a los datos de tareas
    private final TareaRepository tareas;
    // Acceso a los datos de membresías
    private final MembresiaGrupoRepository membresias;
    // Acceso a los datos de recursos
    private final RecursoRepository recursos;
    // Reloj del sistema
    private final Clock reloj;

    // Spring entrega las dependencias por el constructor
    public ResumenService(TareaRepository tareas, MembresiaGrupoRepository membresias, RecursoRepository recursos, Clock reloj) {
        // Guarda el repositorio de tareas
        this.tareas = tareas;
        // Guarda el repositorio de membresías
        this.membresias = membresias;
        // Guarda el repositorio de recursos
        this.recursos = recursos;
        // Guarda el reloj
        this.reloj = reloj;
    }

    // Solo lee datos, así que la transacción es de solo lectura
    @Transactional(readOnly = true)
    // Calcula el resumen del usuario con cuatro consultas en total
    public ResumenAcademico obtener(Long usuarioId) {
        // Cantidad de tareas por estado, obtenida con una sola consulta agrupada
        Map<EstadoTarea, Long> porEstado = new EnumMap<>(EstadoTarea.class);
        // Guarda cada conteo en el mapa
        tareas.contarPorEstado(usuarioId).forEach(conteo -> porEstado.put(conteo.getEstado(), conteo.getTotal()));
        // Tareas pendientes
        int pendientes = cantidad(porEstado, EstadoTarea.PENDIENTE);
        // Tareas en progreso
        int enProgreso = cantidad(porEstado, EstadoTarea.EN_PROGRESO);
        // Tareas hechas
        int hechas = cantidad(porEstado, EstadoTarea.HECHA);
        // Total de tareas
        int total = pendientes + enProgreso + hechas;
        // Sin tareas el porcentaje es cero, así se evita dividir entre cero
        int porcentaje = total == 0 ? 0 : hechas * 100 / total;
        // Próxima tarea sin terminar cuya fecha límite todavía no pasó
        ResumenAcademico.ProximaEntrega proxima = tareas
                .findFirstByResponsableIdAndEstadoNotAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAsc(usuarioId, EstadoTarea.HECHA, Instant.now(reloj))
                .map(tarea -> new ResumenAcademico.ProximaEntrega(tarea.getTitulo(), tarea.getFechaVencimiento()))
                .orElse(null);
        // Arma el resumen con los grupos y los recursos del usuario
        return new ResumenAcademico(total, pendientes, enProgreso, hechas, porcentaje,
                Math.toIntExact(membresias.countByUsuarioId(usuarioId)), Math.toIntExact(recursos.countByAutorId(usuarioId)), proxima);
    }

    // Devuelve el conteo de un estado o cero si no hay tareas en él
    private int cantidad(Map<EstadoTarea, Long> porEstado, EstadoTarea estado) {
        // Convierte el conteo a número entero
        return Math.toIntExact(porEstado.getOrDefault(estado, 0L));
    }
}
