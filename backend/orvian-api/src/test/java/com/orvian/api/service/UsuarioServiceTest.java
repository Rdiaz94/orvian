// Paquete de las pruebas de la capa de servicio
package com.orvian.api.service;

// Verificaciones legibles de AssertJ
import static org.assertj.core.api.Assertions.assertThat;
// Comodín para cualquier argumento en Mockito
import static org.mockito.ArgumentMatchers.any;
// Verifica que un método se llamó
import static org.mockito.Mockito.verify;
// Define el comportamiento de un objeto simulado
import static org.mockito.Mockito.when;

// Anotación que marca un método como prueba
import org.junit.jupiter.api.Test;
// Anotación para ejecutar la prueba con Mockito
import org.junit.jupiter.api.extension.ExtendWith;
// Captura el argumento con el que se llamó a un método
import org.mockito.ArgumentCaptor;
// Crea un objeto simulado
import org.mockito.Mock;
// Inyecta los simulados en la clase a probar
import org.mockito.InjectMocks;
// Extensión de Mockito para JUnit 5
import org.mockito.junit.jupiter.MockitoExtension;
// Resultado paginado de una consulta
import org.springframework.data.domain.Page;
// Datos de la página solicitada
import org.springframework.data.domain.Pageable;
// Define el orden de una consulta
import org.springframework.data.domain.Sort;

// Entidad de usuario
import com.orvian.api.entity.Usuario;
// Repositorio de usuarios
import com.orvian.api.repository.UsuarioRepository;

// Activa Mockito en esta clase de pruebas
@ExtendWith(MockitoExtension.class)
// Pruebas del servicio de usuarios sin base de datos
class UsuarioServiceTest {

    // Repositorio simulado: no toca la base de datos
    @Mock
    private UsuarioRepository usuarios;

    // Servicio real al que se le inyecta el simulado
    @InjectMocks
    private UsuarioService servicio;

    // Marca el método como prueba
    @Test
    // Un tamaño exagerado debe recortarse al máximo permitido
    void limitaElTamanoDePaginaAlMaximo() {
        // El repositorio devuelve una página vacía
        when(usuarios.findAll(any(Pageable.class))).thenReturn(Page.<Usuario>empty());

        // Pide mil usuarios de una sola vez
        servicio.listar(0, 1000);

        // Captura la página que realmente se consultó
        ArgumentCaptor<Pageable> captura = ArgumentCaptor.forClass(Pageable.class);
        // Verifica la consulta y obtiene el argumento
        verify(usuarios).findAll(captura.capture());
        // El tamaño se recortó a 50
        assertThat(captura.getValue().getPageSize()).isEqualTo(50);
    }

    // Marca el método como prueba
    @Test
    // Los valores inválidos deben corregirse y el orden debe ser fijo
    void corrigeValoresInvalidosYUsaOrdenFijo() {
        // El repositorio devuelve una página vacía
        when(usuarios.findAll(any(Pageable.class))).thenReturn(Page.<Usuario>empty());

        // Pide la página -5 con tamaño 0
        servicio.listar(-5, 0);

        // Captura la página que realmente se consultó
        ArgumentCaptor<Pageable> captura = ArgumentCaptor.forClass(Pageable.class);
        // Verifica la consulta y obtiene el argumento
        verify(usuarios).findAll(captura.capture());
        // La página negativa se corrigió a cero
        assertThat(captura.getValue().getPageNumber()).isZero();
        // El tamaño mínimo es uno
        assertThat(captura.getValue().getPageSize()).isEqualTo(1);
        // El orden siempre es por identificador
        assertThat(captura.getValue().getSort()).isEqualTo(Sort.by("id"));
    }
}
