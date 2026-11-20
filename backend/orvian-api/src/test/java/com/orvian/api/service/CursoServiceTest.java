// Paquete de las pruebas de la capa de servicio
package com.orvian.api.service;

// Verificaciones legibles de AssertJ
import static org.assertj.core.api.Assertions.assertThat;
// Verificación de excepciones de AssertJ
import static org.assertj.core.api.Assertions.assertThatThrownBy;
// Comodín para cualquier argumento en Mockito
import static org.mockito.ArgumentMatchers.any;
// Verifica que un método nunca se llamó
import static org.mockito.Mockito.never;
// Verifica que un método se llamó
import static org.mockito.Mockito.verify;
// Define el comportamiento de un objeto simulado
import static org.mockito.Mockito.when;

// Lista de elementos
import java.util.List;

// Anotación que marca un método como prueba
import org.junit.jupiter.api.Test;
// Anotación para ejecutar la prueba con Mockito
import org.junit.jupiter.api.extension.ExtendWith;
// Crea un objeto simulado
import org.mockito.Mock;
// Inyecta los simulados en la clase a probar
import org.mockito.InjectMocks;
// Extensión de Mockito para JUnit 5
import org.mockito.junit.jupiter.MockitoExtension;

// Datos de entrada para crear un curso
import com.orvian.api.dto.CursoRequest;
// Datos públicos del curso
import com.orvian.api.dto.CursoResponse;
// Entidad de curso
import com.orvian.api.entity.Curso;
// Excepción de conflicto con datos existentes
import com.orvian.api.exception.ConflictoException;
// Repositorio de cursos
import com.orvian.api.repository.CursoRepository;

// Activa Mockito en esta clase de pruebas
@ExtendWith(MockitoExtension.class)
// Pruebas del servicio de cursos sin base de datos
class CursoServiceTest {

    // Repositorio simulado: no toca la base de datos
    @Mock
    private CursoRepository cursos;

    // Servicio real al que se le inyecta el simulado
    @InjectMocks
    private CursoService servicio;

    // Marca el método como prueba
    @Test
    // Debe guardar el nombre sin espacios sobrantes
    void creaUnCursoConElNombreSinEspacios() {
        // El curso todavía no existe
        when(cursos.existsByNombreIgnoreCase("Soluciones Web")).thenReturn(false);
        // El repositorio devuelve el mismo curso que recibe
        when(cursos.save(any(Curso.class))).thenAnswer(llamada -> llamada.getArgument(0));

        // Crea el curso con espacios alrededor del nombre
        CursoResponse respuesta = servicio.crear(new CursoRequest("  Soluciones Web  "));

        // El nombre quedó limpio
        assertThat(respuesta.nombre()).isEqualTo("Soluciones Web");
    }

    // Marca el método como prueba
    @Test
    // Debe rechazar un curso repetido y no guardar nada
    void rechazaUnCursoRepetido() {
        // El curso ya existe
        when(cursos.existsByNombreIgnoreCase("Soluciones Web")).thenReturn(true);

        // La creación debe fallar con un conflicto
        assertThatThrownBy(() -> servicio.crear(new CursoRequest("Soluciones Web"))).isInstanceOf(ConflictoException.class);
        // Nunca se debe guardar un curso
        verify(cursos, never()).save(any(Curso.class));
    }

    // Marca el método como prueba
    @Test
    // Debe devolver los cursos en el orden que entrega el repositorio
    void listaLosCursosOrdenados() {
        // El repositorio devuelve dos cursos ya ordenados
        when(cursos.findAllByOrderByNombreAsc()).thenReturn(List.of(new Curso("Base de Datos"), new Curso("Soluciones Web")));

        // Consulta la lista
        List<CursoResponse> lista = servicio.listar();

        // Se conservan los nombres y el orden
        assertThat(lista).extracting(CursoResponse::nombre).containsExactly("Base de Datos", "Soluciones Web");
    }
}
