// Paquete de las pruebas de la capa de servicio
package com.orvian.api.service;

// Verificaciones legibles de AssertJ
import static org.assertj.core.api.Assertions.assertThat;
// Comodín para cualquier argumento en Mockito
import static org.mockito.ArgumentMatchers.any;
// Comodín para un valor exacto en Mockito
import static org.mockito.ArgumentMatchers.eq;
// Define el comportamiento de un objeto simulado
import static org.mockito.Mockito.when;

// Reloj que se puede fijar en una fecha concreta
import java.time.Clock;
// Importa la fecha y hora con precisión de instante
import java.time.Instant;
// Zona horaria UTC
import java.time.ZoneOffset;
// Lista de elementos
import java.util.List;
// Contenedor para un valor que puede no existir
import java.util.Optional;

// Anotación que ejecuta un método antes de cada prueba
import org.junit.jupiter.api.BeforeEach;
// Anotación que marca un método como prueba
import org.junit.jupiter.api.Test;
// Anotación para ejecutar la prueba con Mockito
import org.junit.jupiter.api.extension.ExtendWith;
// Crea un objeto simulado
import org.mockito.Mock;
// Extensión de Mockito para JUnit 5
import org.mockito.junit.jupiter.MockitoExtension;

// Resumen de la actividad académica
import com.orvian.api.dto.ResumenAcademico;
// Estado de la tarea
import com.orvian.api.entity.EstadoTarea;
// Entidad de tarea
import com.orvian.api.entity.Tarea;
// Entidad de usuario
import com.orvian.api.entity.Usuario;
// Repositorio de membresías
import com.orvian.api.repository.MembresiaGrupoRepository;
// Repositorio de recursos
import com.orvian.api.repository.RecursoRepository;
// Repositorio de tareas
import com.orvian.api.repository.TareaRepository;
// Resultado del conteo por estado
import com.orvian.api.repository.TareaRepository.ConteoPorEstado;

// Activa Mockito en esta clase de pruebas
@ExtendWith(MockitoExtension.class)
// Pruebas del cálculo del resumen sin base de datos
class ResumenServiceTest {

    // Identificador del usuario en las pruebas
    private static final Long USUARIO = 1L;
    // Momento fijo que se usa como "ahora"
    private static final Instant AHORA = Instant.parse("2026-11-01T12:00:00Z");

    // Repositorio de tareas simulado
    @Mock
    private TareaRepository tareas;

    // Repositorio de membresías simulado
    @Mock
    private MembresiaGrupoRepository membresias;

    // Repositorio de recursos simulado
    @Mock
    private RecursoRepository recursos;

    // Servicio real que se arma con los simulados y un reloj fijo
    private ResumenService servicio;

    // Se ejecuta antes de cada prueba
    @BeforeEach
    // Arma el servicio con un reloj que siempre marca el mismo momento
    void preparar() {
        // Construye el servicio con el reloj fijo
        servicio = new ResumenService(tareas, membresias, recursos, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    // Crea un conteo de tareas para un estado
    private ConteoPorEstado conteo(EstadoTarea estado, long total) {
        // Implementación mínima de la interfaz de resultado
        return new ConteoPorEstado() {
            // Devuelve el estado
            @Override
            public EstadoTarea getEstado() {
                // Retorna el estado
                return estado;
            }

            // Devuelve el total
            @Override
            public long getTotal() {
                // Retorna el total
                return total;
            }
        };
    }

    // Marca el método como prueba
    @Test
    // Debe sumar los estados, calcular el porcentaje y contar grupos y recursos
    void calculaTotalesYPorcentaje() {
        // El usuario tiene 2 pendientes, 1 en progreso y 1 hecha
        when(tareas.contarPorEstado(USUARIO)).thenReturn(List.of(conteo(EstadoTarea.PENDIENTE, 2), conteo(EstadoTarea.EN_PROGRESO, 1), conteo(EstadoTarea.HECHA, 1)));
        // No hay próxima entrega
        when(tareas.findFirstByResponsableIdAndEstadoNotAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAsc(eq(USUARIO), eq(EstadoTarea.HECHA), any())).thenReturn(Optional.empty());
        // Pertenece a 3 grupos
        when(membresias.countByUsuarioId(USUARIO)).thenReturn(3L);
        // Compartió 5 recursos
        when(recursos.countByAutorId(USUARIO)).thenReturn(5L);

        // Calcula el resumen
        ResumenAcademico resumen = servicio.obtener(USUARIO);

        // El total es la suma de los estados
        assertThat(resumen.tareasTotales()).isEqualTo(4);
        // Los pendientes coinciden
        assertThat(resumen.tareasPendientes()).isEqualTo(2);
        // Los que están en progreso coinciden
        assertThat(resumen.tareasEnProgreso()).isEqualTo(1);
        // Las hechas coinciden
        assertThat(resumen.tareasHechas()).isEqualTo(1);
        // Una de cuatro es el 25 %
        assertThat(resumen.porcentajeCumplimiento()).isEqualTo(25);
        // Los grupos coinciden
        assertThat(resumen.gruposActivos()).isEqualTo(3);
        // Los recursos coinciden
        assertThat(resumen.recursosCompartidos()).isEqualTo(5);
        // No hay próxima entrega
        assertThat(resumen.proximaEntrega()).isNull();
    }

    // Marca el método como prueba
    @Test
    // Sin tareas no debe dividir entre cero
    void sinTareasElPorcentajeEsCero() {
        // El usuario no tiene tareas
        when(tareas.contarPorEstado(USUARIO)).thenReturn(List.of());
        // No hay próxima entrega
        when(tareas.findFirstByResponsableIdAndEstadoNotAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAsc(eq(USUARIO), eq(EstadoTarea.HECHA), any())).thenReturn(Optional.empty());
        // No pertenece a grupos
        when(membresias.countByUsuarioId(USUARIO)).thenReturn(0L);
        // No compartió recursos
        when(recursos.countByAutorId(USUARIO)).thenReturn(0L);

        // Calcula el resumen
        ResumenAcademico resumen = servicio.obtener(USUARIO);

        // Todo queda en cero
        assertThat(resumen.tareasTotales()).isZero();
        // El porcentaje es cero
        assertThat(resumen.porcentajeCumplimiento()).isZero();
    }

    // Marca el método como prueba
    @Test
    // Debe devolver la próxima entrega y consultar a partir del momento actual del reloj
    void incluyeLaProximaEntrega() {
        // El usuario tiene una tarea
        when(tareas.contarPorEstado(USUARIO)).thenReturn(List.of(conteo(EstadoTarea.PENDIENTE, 1)));
        // Tarea con fecha límite futura
        Tarea tarea = new Tarea("Capítulo 1", new Usuario("Luis", "luis@correo.com", "hash", null, null));
        // Le asigna la fecha límite
        tarea.setFechaVencimiento(Instant.parse("2026-11-02T23:59:00Z"));
        // La consulta debe recibir el "ahora" del reloj fijo
        when(tareas.findFirstByResponsableIdAndEstadoNotAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAsc(USUARIO, EstadoTarea.HECHA, AHORA)).thenReturn(Optional.of(tarea));
        // No pertenece a grupos
        when(membresias.countByUsuarioId(USUARIO)).thenReturn(0L);
        // No compartió recursos
        when(recursos.countByAutorId(USUARIO)).thenReturn(0L);

        // Calcula el resumen
        ResumenAcademico resumen = servicio.obtener(USUARIO);

        // La próxima entrega es la tarea
        assertThat(resumen.proximaEntrega().titulo()).isEqualTo("Capítulo 1");
        // Y conserva su fecha límite
        assertThat(resumen.proximaEntrega().fechaVencimiento()).isEqualTo(Instant.parse("2026-11-02T23:59:00Z"));
    }
}
