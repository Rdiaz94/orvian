// Paquete de las pruebas de la capa de servicio
package com.orvian.api.service;

// Verificaciones legibles de AssertJ
import static org.assertj.core.api.Assertions.assertThat;
// Verificación de excepciones de AssertJ
import static org.assertj.core.api.Assertions.assertThatThrownBy;
// Comodín para cualquier argumento en Mockito
import static org.mockito.ArgumentMatchers.any;
// Comodín para un valor exacto en Mockito
import static org.mockito.ArgumentMatchers.eq;
// Verifica que un método nunca se llamó
import static org.mockito.Mockito.never;
// Verifica que un método se llamó
import static org.mockito.Mockito.verify;
// Define el comportamiento de un objeto simulado
import static org.mockito.Mockito.when;

// Colección genérica
import java.util.Collection;
// Contenedor para un valor que puede no existir
import java.util.Optional;

// Anotación que ejecuta un método antes de cada prueba
import org.junit.jupiter.api.BeforeEach;
// Anotación que marca un método como prueba
import org.junit.jupiter.api.Test;
// Anotación para ejecutar la prueba con Mockito
import org.junit.jupiter.api.extension.ExtendWith;
// Captura el argumento con el que se llamó a un método
import org.mockito.ArgumentCaptor;
// Crea un capturador administrado por Mockito
import org.mockito.Captor;
// Crea un objeto simulado
import org.mockito.Mock;
// Extensión de Mockito para JUnit 5
import org.mockito.junit.jupiter.MockitoExtension;
// Resultado paginado de una consulta
import org.springframework.data.domain.Page;
// Datos de la página solicitada
import org.springframework.data.domain.Pageable;
// Utilidad para asignar campos privados en pruebas
import org.springframework.test.util.ReflectionTestUtils;

// Respuesta al registrar una descarga
import com.orvian.api.dto.DescargaResponse;
// Orden permitido para la biblioteca
import com.orvian.api.dto.OrdenRecurso;
// Datos de entrada para compartir o editar un recurso
import com.orvian.api.dto.RecursoRequest;
// Datos públicos del recurso
import com.orvian.api.dto.RecursoResponse;
// Entidad de grupo
import com.orvian.api.entity.Grupo;
// Entidad de membresía
import com.orvian.api.entity.MembresiaGrupo;
// Entidad de recurso
import com.orvian.api.entity.Recurso;
// Papel dentro del grupo
import com.orvian.api.entity.RolGrupo;
// Tipo de recurso
import com.orvian.api.entity.TipoRecurso;
// Entidad de usuario
import com.orvian.api.entity.Usuario;
// Excepción cuando un recurso no existe o es ajeno
import com.orvian.api.exception.NoEncontradoException;
// Excepción cuando falta el permiso para la acción
import com.orvian.api.exception.PermisoInsuficienteException;
// Excepción cuando se incumple una regla de negocio
import com.orvian.api.exception.SolicitudInvalidaException;
// Repositorio de membresías
import com.orvian.api.repository.MembresiaGrupoRepository;
// Repositorio de recursos
import com.orvian.api.repository.RecursoRepository;
// Repositorio de usuarios
import com.orvian.api.repository.UsuarioRepository;

// Activa Mockito en esta clase de pruebas
@ExtendWith(MockitoExtension.class)
// Pruebas del servicio de recursos sin base de datos
class RecursoServiceTest {

    // Identificador del usuario autenticado en las pruebas
    private static final Long USUARIO = 1L;
    // Identificador de otro usuario en las pruebas
    private static final Long OTRO = 2L;
    // Identificador de un recurso en las pruebas
    private static final Long RECURSO = 5L;
    // Identificador de un grupo en las pruebas
    private static final Long GRUPO = 7L;

    // Repositorio de recursos simulado
    @Mock
    private RecursoRepository recursos;

    // Repositorio de usuarios simulado
    @Mock
    private UsuarioRepository usuarios;

    // Servicio de cursos simulado
    @Mock
    private CursoService cursos;

    // Repositorio de membresías simulado
    @Mock
    private MembresiaGrupoRepository membresias;

    // Capturador de los tipos con los que se consultó
    @Captor
    private ArgumentCaptor<Collection<TipoRecurso>> tipos;

    // Capturador de la página con la que se consultó
    @Captor
    private ArgumentCaptor<Pageable> pagina;

    // Servicio real que se arma con los simulados y las reglas de acceso reales
    private RecursoService servicio;

    // Usuario autenticado de las pruebas
    private final Usuario yo = new Usuario("Luis", "luis@correo.com", "hash", "Ingeniería", 4);
    // Otro usuario de las pruebas
    private final Usuario otro = new Usuario("Ana", "ana@correo.com", "hash", "Ingeniería", 5);
    // Grupo de ejemplo
    private final Grupo grupo = new Grupo("Orvian Team", otro);

    // Se ejecuta antes de cada prueba
    @BeforeEach
    // Arma el servicio con el mismo cableado que usa la aplicación y asigna identificadores
    void preparar() {
        // Asigna el identificador que normalmente pone la base de datos
        ReflectionTestUtils.setField(yo, "id", USUARIO);
        // Asigna el identificador del otro usuario
        ReflectionTestUtils.setField(otro, "id", OTRO);
        // Asigna el identificador del grupo
        ReflectionTestUtils.setField(grupo, "id", GRUPO);
        // Las reglas de acceso son reales y consultan el repositorio simulado
        servicio = new RecursoService(recursos, usuarios, cursos, new AccesoGrupo(membresias));
    }

    // Crea un recurso de ejemplo del autor indicado
    private Recurso recursoDe(Usuario autor) {
        // Recurso con enlace válido
        return new Recurso("Guía", TipoRecurso.PDF, "https://ejemplo.com/guia.pdf", autor);
    }

    // Deja disponible la membresía del usuario autenticado con el papel indicado
    private void yoComo(RolGrupo rol) {
        // La consulta por grupo y usuario devuelve la membresía
        when(membresias.findByGrupoIdAndUsuarioId(GRUPO, USUARIO)).thenReturn(Optional.of(new MembresiaGrupo(grupo, yo, rol)));
    }

    // Marca el método como prueba
    @Test
    // Sin filtros debe considerar todos los tipos, texto vacío y el orden pedido
    void listaConTodosLosTiposYTextoVacioPorDefecto() {
        // El repositorio devuelve una página vacía
        when(recursos.buscarVisibles(eq(USUARIO), any(), eq(""), any(Pageable.class))).thenReturn(Page.<Recurso>empty());

        // Lista los más descargados sin filtros
        servicio.listar(USUARIO, null, null, OrdenRecurso.POPULARES, 0, 20);

        // Captura los argumentos de la consulta
        verify(recursos).buscarVisibles(eq(USUARIO), tipos.capture(), eq(""), pagina.capture());
        // Se consideraron todos los tipos
        assertThat(tipos.getValue()).containsExactlyInAnyOrder(TipoRecurso.values());
        // El orden es el de los más descargados, definido por el servidor
        assertThat(pagina.getValue().getSort()).isEqualTo(OrdenRecurso.POPULARES.orden());
    }

    // Marca el método como prueba
    @Test
    // Con filtros debe consultar solo ese tipo y el texto sin espacios
    void filtraPorTipoYTexto() {
        // El repositorio devuelve una página vacía
        when(recursos.buscarVisibles(eq(USUARIO), any(), eq("java"), any(Pageable.class))).thenReturn(Page.<Recurso>empty());

        // Lista solo videos que contengan "java"
        servicio.listar(USUARIO, TipoRecurso.VIDEO, "  java ", OrdenRecurso.RECIENTES, 0, 20);

        // Captura los tipos de la consulta
        verify(recursos).buscarVisibles(eq(USUARIO), tipos.capture(), eq("java"), any(Pageable.class));
        // Solo se consultó el tipo pedido
        assertThat(tipos.getValue()).containsExactly(TipoRecurso.VIDEO);
    }

    // Marca el método como prueba
    @Test
    // Un texto demasiado largo debe recortarse
    void recortaElTextoDeBusquedaLargo() {
        // El repositorio devuelve una página vacía
        when(recursos.buscarVisibles(eq(USUARIO), any(), eq("a".repeat(100)), any(Pageable.class))).thenReturn(Page.<Recurso>empty());

        // Busca con 150 caracteres
        servicio.listar(USUARIO, null, "a".repeat(150), OrdenRecurso.RECIENTES, 0, 20);

        // Se consultó con solo 100 caracteres
        verify(recursos).buscarVisibles(eq(USUARIO), any(), eq("a".repeat(100)), any(Pageable.class));
    }

    // Marca el método como prueba
    @Test
    // Un recurso que el usuario no puede ver debe comportarse como si no existiera
    void ocultaLosRecursosNoVisibles() {
        // La consulta con visibilidad no encuentra nada
        when(recursos.buscarVisiblePorId(RECURSO, USUARIO)).thenReturn(Optional.empty());

        // Debe responder que no existe
        assertThatThrownBy(() -> servicio.obtener(USUARIO, RECURSO)).isInstanceOf(NoEncontradoException.class);
        // Nunca se debe buscar por identificador solamente
        verify(recursos, never()).findById(any());
    }

    // Marca el método como prueba
    @Test
    // Un recurso público debe quedar sin grupo, con textos limpios y a nombre del usuario del token
    void creaUnRecursoPublicoConDatosLimpios() {
        // El repositorio entrega la referencia al usuario
        when(usuarios.getReferenceById(USUARIO)).thenReturn(yo);
        // El repositorio devuelve el mismo recurso que recibe
        when(recursos.save(any(Recurso.class))).thenAnswer(llamada -> llamada.getArgument(0));

        // Crea un recurso con espacios alrededor del título y del enlace
        RecursoResponse respuesta = servicio.crear(USUARIO, new RecursoRequest("  Guía  ", TipoRecurso.PDF, "  https://ejemplo.com/guia.pdf ", null, null, null));

        // El título quedó limpio
        assertThat(respuesta.titulo()).isEqualTo("Guía");
        // El enlace quedó limpio
        assertThat(respuesta.url()).isEqualTo("https://ejemplo.com/guia.pdf");
        // No tiene grupo
        assertThat(respuesta.grupoId()).isNull();
        // El autor es quien tiene la sesión
        assertThat(respuesta.esAutor()).isTrue();
    }

    // Marca el método como prueba
    @Test
    // No se puede compartir en un grupo al que no se pertenece
    void noComparteEnUnGrupoAjeno() {
        // La consulta por grupo y usuario no encuentra nada
        when(membresias.findByGrupoIdAndUsuarioId(GRUPO, USUARIO)).thenReturn(Optional.empty());

        // Debe responder que el grupo no existe
        assertThatThrownBy(() -> servicio.crear(USUARIO, new RecursoRequest("Guía", TipoRecurso.PDF, "https://ejemplo.com/g.pdf", null, null, GRUPO))).isInstanceOf(NoEncontradoException.class);
        // Nunca se debe guardar el recurso
        verify(recursos, never()).save(any(Recurso.class));
    }

    // Marca el método como prueba
    @Test
    // Un miembro común sí puede compartir en su grupo
    void unMiembroComparteEnSuGrupo() {
        // El usuario es miembro común
        yoComo(RolGrupo.MIEMBRO);
        // El repositorio entrega la referencia al usuario
        when(usuarios.getReferenceById(USUARIO)).thenReturn(yo);
        // El repositorio devuelve el mismo recurso que recibe
        when(recursos.save(any(Recurso.class))).thenAnswer(llamada -> llamada.getArgument(0));

        // Comparte un recurso en el grupo
        RecursoResponse respuesta = servicio.crear(USUARIO, new RecursoRequest("Guía", TipoRecurso.PDF, "https://ejemplo.com/g.pdf", null, null, GRUPO));

        // El recurso quedó en el grupo
        assertThat(respuesta.grupoNombre()).isEqualTo("Orvian Team");
    }

    // Marca el método como prueba
    @Test
    // Solo el autor puede editar un recurso
    void soloElAutorEditaUnRecurso() {
        // Recurso visible cuyo autor es otra persona
        when(recursos.buscarVisiblePorId(RECURSO, USUARIO)).thenReturn(Optional.of(recursoDe(otro)));

        // Debe fallar por falta de permiso
        assertThatThrownBy(() -> servicio.actualizar(USUARIO, RECURSO, new RecursoRequest("Nuevo", TipoRecurso.PDF, "https://ejemplo.com/n.pdf", null, null, null))).isInstanceOf(PermisoInsuficienteException.class);
    }

    // Marca el método como prueba
    @Test
    // Editar no puede cambiar el grupo, porque permitiría publicar contenido privado
    void noCambiaElGrupoAlEditar() {
        // Recurso propio que pertenece a un grupo
        Recurso recurso = recursoDe(yo);
        // Lo vincula al grupo
        recurso.setGrupo(grupo);
        // La consulta con visibilidad lo devuelve
        when(recursos.buscarVisiblePorId(RECURSO, USUARIO)).thenReturn(Optional.of(recurso));

        // Intentar volverlo público debe fallar
        assertThatThrownBy(() -> servicio.actualizar(USUARIO, RECURSO, new RecursoRequest("Guía", TipoRecurso.PDF, "https://ejemplo.com/g.pdf", null, null, null))).isInstanceOf(SolicitudInvalidaException.class);
    }

    // Marca el método como prueba
    @Test
    // La descarga suma una vez y devuelve el enlace
    void laDescargaSumaYDevuelveElEnlace() {
        // Recurso visible
        when(recursos.buscarVisiblePorId(RECURSO, USUARIO)).thenReturn(Optional.of(recursoDe(otro)));

        // Registra la descarga
        DescargaResponse respuesta = servicio.registrarDescarga(USUARIO, RECURSO);

        // Se sumó una descarga con la consulta atómica
        verify(recursos).incrementarDescargas(RECURSO);
        // Devuelve el enlace
        assertThat(respuesta.url()).isEqualTo("https://ejemplo.com/guia.pdf");
    }

    // Marca el método como prueba
    @Test
    // No se registra una descarga de un recurso que el usuario no puede ver
    void noRegistraDescargasDeRecursosNoVisibles() {
        // La consulta con visibilidad no encuentra nada
        when(recursos.buscarVisiblePorId(RECURSO, USUARIO)).thenReturn(Optional.empty());

        // Debe responder que no existe
        assertThatThrownBy(() -> servicio.registrarDescarga(USUARIO, RECURSO)).isInstanceOf(NoEncontradoException.class);
        // Nunca se debe sumar la descarga
        verify(recursos, never()).incrementarDescargas(any());
    }

    // Marca el método como prueba
    @Test
    // El autor puede borrar su recurso
    void elAutorBorraSuRecurso() {
        // Recurso propio
        Recurso recurso = recursoDe(yo);
        // La consulta con visibilidad lo devuelve
        when(recursos.buscarVisiblePorId(RECURSO, USUARIO)).thenReturn(Optional.of(recurso));

        // Lo borra
        servicio.eliminar(USUARIO, RECURSO);

        // Se borró el recurso
        verify(recursos).delete(recurso);
    }

    // Marca el método como prueba
    @Test
    // Un recurso público ajeno no se puede borrar
    void unRecursoPublicoAjenoNoSeBorra() {
        // Recurso público de otra persona
        when(recursos.buscarVisiblePorId(RECURSO, USUARIO)).thenReturn(Optional.of(recursoDe(otro)));

        // Debe fallar por falta de permiso
        assertThatThrownBy(() -> servicio.eliminar(USUARIO, RECURSO)).isInstanceOf(PermisoInsuficienteException.class);
        // Nunca se debe borrar nada
        verify(recursos, never()).delete(any(Recurso.class));
    }

    // Marca el método como prueba
    @Test
    // Un miembro común no puede borrar recursos que compartió otra persona
    void unMiembroNoBorraRecursosAjenosDeGrupo() {
        // El usuario es miembro común
        yoComo(RolGrupo.MIEMBRO);
        // Recurso de grupo de otra persona
        Recurso recurso = recursoDe(otro);
        // Lo vincula al grupo
        recurso.setGrupo(grupo);
        // La consulta con visibilidad lo devuelve
        when(recursos.buscarVisiblePorId(RECURSO, USUARIO)).thenReturn(Optional.of(recurso));

        // Debe fallar por falta de permiso
        assertThatThrownBy(() -> servicio.eliminar(USUARIO, RECURSO)).isInstanceOf(PermisoInsuficienteException.class);
        // Nunca se debe borrar nada
        verify(recursos, never()).delete(any(Recurso.class));
    }

    // Marca el método como prueba
    @Test
    // El líder del grupo puede borrar recursos que compartió otro integrante
    void elLiderBorraRecursosDeSuGrupo() {
        // El usuario es líder
        yoComo(RolGrupo.LIDER);
        // Recurso de grupo de otra persona
        Recurso recurso = recursoDe(otro);
        // Lo vincula al grupo
        recurso.setGrupo(grupo);
        // La consulta con visibilidad lo devuelve
        when(recursos.buscarVisiblePorId(RECURSO, USUARIO)).thenReturn(Optional.of(recurso));

        // Lo borra
        servicio.eliminar(USUARIO, RECURSO);

        // Se borró el recurso
        verify(recursos).delete(recurso);
    }
}
