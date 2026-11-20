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
// Crea un objeto simulado
import org.mockito.Mock;
// Extensión de Mockito para JUnit 5
import org.mockito.junit.jupiter.MockitoExtension;
// Utilidad para asignar campos privados en pruebas
import org.springframework.test.util.ReflectionTestUtils;

// Datos de entrada para crear un grupo
import com.orvian.api.dto.GrupoRequest;
// Datos de entrada para invitar a una persona
import com.orvian.api.dto.IntegranteRequest;
// Datos públicos de un integrante
import com.orvian.api.dto.IntegranteResponse;
// Datos públicos del grupo
import com.orvian.api.dto.GrupoResponse;
// Entidad de grupo
import com.orvian.api.entity.Grupo;
// Entidad de membresía
import com.orvian.api.entity.MembresiaGrupo;
// Papel dentro del grupo
import com.orvian.api.entity.RolGrupo;
// Entidad de usuario
import com.orvian.api.entity.Usuario;
// Excepción de conflicto con datos existentes
import com.orvian.api.exception.ConflictoException;
// Excepción cuando un recurso no existe o es ajeno
import com.orvian.api.exception.NoEncontradoException;
// Excepción cuando falta el permiso para la acción
import com.orvian.api.exception.PermisoInsuficienteException;
// Excepción cuando se incumple una regla de negocio
import com.orvian.api.exception.SolicitudInvalidaException;
// Repositorio de grupos
import com.orvian.api.repository.GrupoRepository;
// Repositorio de membresías
import com.orvian.api.repository.MembresiaGrupoRepository;
// Repositorio de recursos
import com.orvian.api.repository.RecursoRepository;
// Repositorio de tareas
import com.orvian.api.repository.TareaRepository;
// Repositorio de usuarios
import com.orvian.api.repository.UsuarioRepository;

// Activa Mockito en esta clase de pruebas
@ExtendWith(MockitoExtension.class)
// Pruebas del servicio de grupos sin base de datos
class GrupoServiceTest {

    // Identificador del usuario autenticado en las pruebas
    private static final Long YO = 1L;
    // Identificador de otro integrante en las pruebas
    private static final Long OTRO = 2L;
    // Identificador del grupo en las pruebas
    private static final Long GRUPO = 7L;

    // Repositorio de grupos simulado
    @Mock
    private GrupoRepository grupos;

    // Repositorio de membresías simulado
    @Mock
    private MembresiaGrupoRepository membresias;

    // Repositorio de usuarios simulado
    @Mock
    private UsuarioRepository usuarios;

    // Servicio de cursos simulado
    @Mock
    private CursoService cursos;

    // Repositorio de tareas simulado
    @Mock
    private TareaRepository tareas;

    // Repositorio de recursos simulado
    @Mock
    private RecursoRepository recursos;

    // Servicio real que se arma con los simulados y las reglas de acceso reales
    private GrupoService servicio;

    // Se ejecuta antes de cada prueba
    @BeforeEach
    // Arma el servicio con el mismo cableado que usa la aplicación
    void preparar() {
        // Las reglas de acceso son reales y consultan el repositorio simulado
        servicio = new GrupoService(grupos, membresias, usuarios, cursos, new AccesoGrupo(membresias), tareas, recursos);
    }

    // Usuario que realiza las acciones
    private final Usuario yo = new Usuario("Luis", "luis@correo.com", "hash", "Ingeniería", 4);
    // Grupo de ejemplo
    private final Grupo grupo = new Grupo("Orvian Team", yo);

    // Crea la membresía del usuario autenticado con el papel indicado y la deja disponible para las consultas
    private MembresiaGrupo yoComo(RolGrupo rol) {
        // Crea la membresía
        MembresiaGrupo propia = new MembresiaGrupo(grupo, yo, rol);
        // La consulta por grupo y usuario la devuelve
        when(membresias.findByGrupoIdAndUsuarioId(GRUPO, YO)).thenReturn(Optional.of(propia));
        // Devuelve la membresía
        return propia;
    }

    // Marca el método como prueba
    @Test
    // El creador debe quedar registrado como líder
    void creaElGrupoYHaceLiderAlCreador() {
        // El repositorio entrega la referencia al usuario
        when(usuarios.getReferenceById(YO)).thenReturn(yo);

        // Crea un grupo con espacios en el nombre
        GrupoResponse respuesta = servicio.crear(YO, new GrupoRequest("  Orvian Team  ", null, null));

        // Captura la membresía que se guardó
        ArgumentCaptor<MembresiaGrupo> captura = ArgumentCaptor.forClass(MembresiaGrupo.class);
        // Verifica que se guardó una vez y obtiene la membresía
        verify(membresias).save(captura.capture());
        // El papel es líder
        assertThat(captura.getValue().getRolGrupo()).isEqualTo(RolGrupo.LIDER);
        // El integrante es el usuario del token
        assertThat(captura.getValue().getUsuario()).isSameAs(yo);
        // El nombre quedó limpio
        assertThat(respuesta.nombre()).isEqualTo("Orvian Team");
        // La respuesta indica que es líder
        assertThat(respuesta.miRol()).isEqualTo(RolGrupo.LIDER);
    }

    // Marca el método como prueba
    @Test
    // Quien no pertenece a un grupo no debe saber ni que existe
    void ocultaLosGruposAjenos() {
        // La consulta por grupo y usuario no encuentra nada
        when(membresias.findByGrupoIdAndUsuarioId(GRUPO, YO)).thenReturn(Optional.empty());

        // Debe responder que no existe
        assertThatThrownBy(() -> servicio.obtener(YO, GRUPO)).isInstanceOf(NoEncontradoException.class);
    }

    // Marca el método como prueba
    @Test
    // Un miembro común no puede invitar a nadie
    void soloElLiderAgregaIntegrantes() {
        // El usuario es miembro común
        yoComo(RolGrupo.MIEMBRO);

        // Debe fallar por falta de permiso
        assertThatThrownBy(() -> servicio.agregarIntegrante(YO, GRUPO, new IntegranteRequest("ana@correo.com"))).isInstanceOf(PermisoInsuficienteException.class);
        // Nunca se debe guardar una membresía
        verify(membresias, never()).save(any(MembresiaGrupo.class));
    }

    // Marca el método como prueba
    @Test
    // Invitar a un correo que no existe debe fallar
    void rechazaInvitarACorreoInexistente() {
        // El usuario es líder
        yoComo(RolGrupo.LIDER);
        // El correo normalizado no existe
        when(usuarios.findByCorreo("nadie@correo.com")).thenReturn(Optional.empty());

        // Debe responder que no existe, aunque el correo venga en mayúsculas
        assertThatThrownBy(() -> servicio.agregarIntegrante(YO, GRUPO, new IntegranteRequest("  NADIE@correo.com "))).isInstanceOf(NoEncontradoException.class);
    }

    // Marca el método como prueba
    @Test
    // No se puede invitar dos veces a la misma persona
    void rechazaUnIntegranteRepetido() {
        // El usuario es líder
        yoComo(RolGrupo.LIDER);
        // Persona a invitar, con identificador
        Usuario ana = new Usuario("Ana", "ana@correo.com", "hash", "Ingeniería", 5);
        // Le asigna el identificador que normalmente pone la base de datos
        ReflectionTestUtils.setField(ana, "id", OTRO);
        // La persona existe
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(ana));
        // Ya es integrante
        when(membresias.existsByGrupoIdAndUsuarioId(GRUPO, OTRO)).thenReturn(true);

        // Debe fallar con un conflicto
        assertThatThrownBy(() -> servicio.agregarIntegrante(YO, GRUPO, new IntegranteRequest("ana@correo.com"))).isInstanceOf(ConflictoException.class);
        // Nunca se debe guardar una membresía
        verify(membresias, never()).save(any(MembresiaGrupo.class));
    }

    // Marca el método como prueba
    @Test
    // El líder puede sumar a una persona como miembro común
    void agregaUnIntegranteComoMiembro() {
        // El usuario es líder
        yoComo(RolGrupo.LIDER);
        // Persona a invitar, con identificador
        Usuario ana = new Usuario("Ana", "ana@correo.com", "hash", "Ingeniería", 5);
        // Le asigna el identificador que normalmente pone la base de datos
        ReflectionTestUtils.setField(ana, "id", OTRO);
        // La persona existe
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(ana));
        // Todavía no es integrante
        when(membresias.existsByGrupoIdAndUsuarioId(GRUPO, OTRO)).thenReturn(false);
        // El repositorio devuelve la misma membresía que recibe
        when(membresias.save(any(MembresiaGrupo.class))).thenAnswer(llamada -> llamada.getArgument(0));

        // Invita a Ana
        IntegranteResponse respuesta = servicio.agregarIntegrante(YO, GRUPO, new IntegranteRequest("ana@correo.com"));

        // Ingresa como miembro común
        assertThat(respuesta.rolGrupo()).isEqualTo(RolGrupo.MIEMBRO);
        // La respuesta no expone el correo, solo datos públicos
        assertThat(respuesta.nombre()).isEqualTo("Ana");
    }

    // Marca el método como prueba
    @Test
    // El líder no puede abandonar su propio grupo
    void elLiderNoPuedeAbandonar() {
        // El usuario es líder
        yoComo(RolGrupo.LIDER);

        // Debe fallar con una regla de negocio
        assertThatThrownBy(() -> servicio.quitarIntegrante(YO, GRUPO, YO)).isInstanceOf(SolicitudInvalidaException.class);
        // Nunca se debe borrar la membresía
        verify(membresias, never()).delete(any(MembresiaGrupo.class));
    }

    // Marca el método como prueba
    @Test
    // Un miembro común no puede expulsar a otro
    void unMiembroNoPuedeExpulsarAOtro() {
        // El usuario es miembro común
        yoComo(RolGrupo.MIEMBRO);

        // Debe fallar por falta de permiso
        assertThatThrownBy(() -> servicio.quitarIntegrante(YO, GRUPO, OTRO)).isInstanceOf(PermisoInsuficienteException.class);
        // Nunca se debe borrar una membresía
        verify(membresias, never()).delete(any(MembresiaGrupo.class));
    }

    // Marca el método como prueba
    @Test
    // Un miembro común sí puede irse por su cuenta
    void unMiembroPuedeAbandonar() {
        // El usuario es miembro común
        MembresiaGrupo propia = yoComo(RolGrupo.MIEMBRO);

        // Abandona el grupo
        servicio.quitarIntegrante(YO, GRUPO, YO);

        // Se borra su propia membresía
        verify(membresias).delete(propia);
    }

    // Marca el método como prueba
    @Test
    // Solo el líder puede eliminar el grupo
    void soloElLiderEliminaElGrupo() {
        // El usuario es miembro común
        yoComo(RolGrupo.MIEMBRO);

        // Debe fallar por falta de permiso
        assertThatThrownBy(() -> servicio.eliminar(YO, GRUPO)).isInstanceOf(PermisoInsuficienteException.class);
        // Nunca se debe borrar el grupo
        verify(grupos, never()).delete(any(Grupo.class));
    }

    // Marca el método como prueba
    @Test
    // Al eliminar el grupo se borran también sus tareas y sus membresías
    void alEliminarElGrupoBorraSusTareas() {
        // El usuario es líder
        yoComo(RolGrupo.LIDER);

        // Elimina el grupo
        servicio.eliminar(YO, GRUPO);

        // Se borraron las tareas del grupo
        verify(tareas).deleteByGrupoId(GRUPO);
        // Se borraron los recursos del grupo
        verify(recursos).deleteByGrupoId(GRUPO);
        // Se borraron las membresías
        verify(membresias).deleteByGrupoId(GRUPO);
        // Se borró el grupo
        verify(grupos).delete(grupo);
    }
}
