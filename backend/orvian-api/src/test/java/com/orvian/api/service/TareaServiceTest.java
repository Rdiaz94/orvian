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
// Resultado paginado de una consulta
import org.springframework.data.domain.Page;
// Datos de la página solicitada
import org.springframework.data.domain.Pageable;
// Utilidad para asignar campos privados en pruebas
import org.springframework.test.util.ReflectionTestUtils;

// Datos de entrada para mover o avanzar una tarea
import com.orvian.api.dto.AvanceRequest;
// Datos de entrada para crear o editar una tarea de grupo
import com.orvian.api.dto.TareaGrupoRequest;
// Datos de entrada para crear o editar una tarea personal
import com.orvian.api.dto.TareaRequest;
// Datos públicos de la tarea
import com.orvian.api.dto.TareaResponse;
// Estado de la tarea
import com.orvian.api.entity.EstadoTarea;
// Etiqueta de la tarea
import com.orvian.api.entity.EtiquetaTarea;
// Entidad de grupo
import com.orvian.api.entity.Grupo;
// Entidad de membresía
import com.orvian.api.entity.MembresiaGrupo;
// Papel dentro del grupo
import com.orvian.api.entity.RolGrupo;
// Entidad de tarea
import com.orvian.api.entity.Tarea;
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
// Repositorio de tareas
import com.orvian.api.repository.TareaRepository;
// Repositorio de usuarios
import com.orvian.api.repository.UsuarioRepository;

// Activa Mockito en esta clase de pruebas
@ExtendWith(MockitoExtension.class)
// Pruebas del servicio de tareas sin base de datos
class TareaServiceTest {

    // Identificador del usuario autenticado en las pruebas
    private static final Long USUARIO = 1L;
    // Identificador de otro usuario en las pruebas
    private static final Long OTRO = 2L;
    // Identificador de una tarea en las pruebas
    private static final Long TAREA = 5L;
    // Identificador de un grupo en las pruebas
    private static final Long GRUPO = 7L;

    // Repositorio de tareas simulado
    @Mock
    private TareaRepository tareas;

    // Repositorio de usuarios simulado
    @Mock
    private UsuarioRepository usuarios;

    // Servicio de cursos simulado
    @Mock
    private CursoService cursos;

    // Repositorio de membresías simulado
    @Mock
    private MembresiaGrupoRepository membresias;

    // Servicio real que se arma con los simulados y las reglas de acceso reales
    private TareaService servicio;

    // Usuario autenticado de las pruebas
    private final Usuario dueno = new Usuario("Luis", "luis@correo.com", "hash", "Ingeniería", 4);
    // Otro usuario de las pruebas
    private final Usuario otro = new Usuario("Ana", "ana@correo.com", "hash", "Ingeniería", 5);
    // Grupo de ejemplo
    private final Grupo grupo = new Grupo("Orvian Team", dueno);

    // Se ejecuta antes de cada prueba
    @BeforeEach
    // Arma el servicio con el mismo cableado que usa la aplicación y asigna identificadores
    void preparar() {
        // Asigna el identificador que normalmente pone la base de datos
        ReflectionTestUtils.setField(dueno, "id", USUARIO);
        // Asigna el identificador del otro usuario
        ReflectionTestUtils.setField(otro, "id", OTRO);
        // Las reglas de acceso son reales y consultan el repositorio simulado
        servicio = new TareaService(tareas, usuarios, cursos, membresias, new AccesoGrupo(membresias));
    }

    // Deja disponible la membresía del usuario autenticado con el papel indicado
    private void yoComo(RolGrupo rol) {
        // La consulta por grupo y usuario devuelve la membresía
        when(membresias.findByGrupoIdAndUsuarioId(GRUPO, USUARIO)).thenReturn(Optional.of(new MembresiaGrupo(grupo, dueno, rol)));
    }

    // Marca el método como prueba
    @Test
    // Sin filtro debe consultar todas las tareas del usuario
    void listaLasTareasDelUsuarioSinFiltro() {
        // El repositorio devuelve una página vacía
        when(tareas.findByResponsableId(eq(USUARIO), any(Pageable.class))).thenReturn(Page.<Tarea>empty());

        // Lista sin filtro de estado
        servicio.listar(USUARIO, null, 0, 20);

        // Debe haberse consultado por el responsable
        verify(tareas).findByResponsableId(eq(USUARIO), any(Pageable.class));
    }

    // Marca el método como prueba
    @Test
    // Con filtro debe consultar solo ese estado del usuario
    void filtraLasTareasPorEstado() {
        // El repositorio devuelve una página vacía
        when(tareas.findByResponsableIdAndEstado(eq(USUARIO), eq(EstadoTarea.HECHA), any(Pageable.class))).thenReturn(Page.<Tarea>empty());

        // Lista solo las tareas hechas
        servicio.listar(USUARIO, EstadoTarea.HECHA, 0, 20);

        // Debe haberse consultado por responsable y estado
        verify(tareas).findByResponsableIdAndEstado(eq(USUARIO), eq(EstadoTarea.HECHA), any(Pageable.class));
    }

    // Marca el método como prueba
    @Test
    // Una tarea ajena debe comportarse como si no existiera
    void noRevelaLasTareasAjenas() {
        // La consulta por responsable no encuentra nada
        when(tareas.findByIdAndResponsableId(TAREA, USUARIO)).thenReturn(Optional.empty());

        // Debe responder que no existe
        assertThatThrownBy(() -> servicio.obtener(USUARIO, TAREA)).isInstanceOf(NoEncontradoException.class);
        // Nunca se debe buscar por identificador solamente
        verify(tareas, never()).findById(any());
    }

    // Marca el método como prueba
    @Test
    // No se puede borrar una tarea ajena
    void noBorraTareasAjenas() {
        // La consulta por responsable no encuentra nada
        when(tareas.findByIdAndResponsableId(TAREA, USUARIO)).thenReturn(Optional.empty());

        // Debe responder que no existe
        assertThatThrownBy(() -> servicio.eliminar(USUARIO, TAREA)).isInstanceOf(NoEncontradoException.class);
        // Nunca se debe borrar nada
        verify(tareas, never()).delete(any(Tarea.class));
    }

    // Marca el método como prueba
    @Test
    // Debe asignar la tarea al usuario del token, limpiar el título y aplicar valores por defecto
    void creaLaTareaParaElUsuarioConValoresPorDefecto() {
        // El repositorio entrega la referencia al usuario
        when(usuarios.getReferenceById(USUARIO)).thenReturn(dueno);
        // El repositorio devuelve la misma tarea que recibe
        when(tareas.save(any(Tarea.class))).thenAnswer(llamada -> llamada.getArgument(0));

        // Crea una tarea con espacios en el título y sin etiqueta
        TareaResponse respuesta = servicio.crear(USUARIO, new TareaRequest("  Capítulo 1  ", null, null, null, null));

        // Captura la tarea que se guardó
        ArgumentCaptor<Tarea> captura = ArgumentCaptor.forClass(Tarea.class);
        // Verifica que se guardó una vez y obtiene la tarea
        verify(tareas).save(captura.capture());
        // El responsable es el usuario del token
        assertThat(captura.getValue().getResponsable()).isSameAs(dueno);
        // El título quedó limpio
        assertThat(respuesta.titulo()).isEqualTo("Capítulo 1");
        // Empieza pendiente y sin avance
        assertThat(respuesta.estado()).isEqualTo(EstadoTarea.PENDIENTE);
        // El avance inicial es cero
        assertThat(respuesta.progreso()).isZero();
        // Sin etiqueta se usa OTRO
        assertThat(respuesta.etiqueta()).isEqualTo(EtiquetaTarea.OTRO);
    }

    // Marca el método como prueba
    @Test
    // No debe crear una tarea con un curso que no existe
    void rechazaUnCursoInexistente() {
        // El repositorio entrega la referencia al usuario
        when(usuarios.getReferenceById(USUARIO)).thenReturn(dueno);
        // El curso 99 no existe, así que el servicio de cursos rechaza la petición
        when(cursos.buscarSiSeIndica(99L)).thenThrow(new SolicitudInvalidaException("El curso indicado no existe"));

        // Debe fallar con una solicitud inválida
        assertThatThrownBy(() -> servicio.crear(USUARIO, new TareaRequest("Tarea", null, 99L, null, null))).isInstanceOf(SolicitudInvalidaException.class);
        // Nunca se debe guardar la tarea
        verify(tareas, never()).save(any(Tarea.class));
    }

    // Marca el método como prueba
    @Test
    // Hecha implica 100 % de avance
    void marcarComoHechaFijaElAvanceEnCien() {
        // Tarea del usuario encontrada
        Tarea tarea = new Tarea("Tarea", dueno);
        // La consulta por responsable la devuelve
        when(tareas.findByIdAndResponsableId(TAREA, USUARIO)).thenReturn(Optional.of(tarea));

        // La marca como hecha
        TareaResponse respuesta = servicio.registrarAvance(USUARIO, TAREA, new AvanceRequest(EstadoTarea.HECHA, null));

        // El avance es 100
        assertThat(respuesta.progreso()).isEqualTo(100);
        // El estado es hecha
        assertThat(respuesta.estado()).isEqualTo(EstadoTarea.HECHA);
    }

    // Marca el método como prueba
    @Test
    // Pendiente implica 0 % de avance, aunque el cliente envíe otro número
    void volverAPendienteReiniciaElAvance() {
        // Tarea del usuario con avance previo
        Tarea tarea = new Tarea("Tarea", dueno);
        // Le asigna un avance
        tarea.setProgreso(60);
        // La consulta por responsable la devuelve
        when(tareas.findByIdAndResponsableId(TAREA, USUARIO)).thenReturn(Optional.of(tarea));

        // La devuelve a pendiente enviando un avance contradictorio
        TareaResponse respuesta = servicio.registrarAvance(USUARIO, TAREA, new AvanceRequest(EstadoTarea.PENDIENTE, 80));

        // El avance se reinicia a cero
        assertThat(respuesta.progreso()).isZero();
    }

    // Marca el método como prueba
    @Test
    // En progreso exige un avance entre 1 y 99
    void enProgresoExigeUnAvanceValido() {
        // Tarea del usuario encontrada
        Tarea tarea = new Tarea("Tarea", dueno);
        // La consulta por responsable la devuelve
        when(tareas.findByIdAndResponsableId(TAREA, USUARIO)).thenReturn(Optional.of(tarea));

        // Sin avance debe fallar
        assertThatThrownBy(() -> servicio.registrarAvance(USUARIO, TAREA, new AvanceRequest(EstadoTarea.EN_PROGRESO, null))).isInstanceOf(SolicitudInvalidaException.class);
        // Con 100 también debe fallar, porque eso sería hecha
        assertThatThrownBy(() -> servicio.registrarAvance(USUARIO, TAREA, new AvanceRequest(EstadoTarea.EN_PROGRESO, 100))).isInstanceOf(SolicitudInvalidaException.class);
    }

    // Marca el método como prueba
    @Test
    // En progreso con un avance válido lo guarda
    void enProgresoGuardaElAvance() {
        // Tarea del usuario encontrada
        Tarea tarea = new Tarea("Tarea", dueno);
        // La consulta por responsable la devuelve
        when(tareas.findByIdAndResponsableId(TAREA, USUARIO)).thenReturn(Optional.of(tarea));

        // La pasa a en progreso con 40 %
        TareaResponse respuesta = servicio.registrarAvance(USUARIO, TAREA, new AvanceRequest(EstadoTarea.EN_PROGRESO, 40));

        // El avance quedó en 40
        assertThat(respuesta.progreso()).isEqualTo(40);
        // El estado es en progreso
        assertThat(respuesta.estado()).isEqualTo(EstadoTarea.EN_PROGRESO);
    }

    // Marca el método como prueba
    @Test
    // Una tarea de grupo asignada al usuario no se edita ni se borra desde las rutas personales
    void lasTareasDeGrupoNoSeGestionanDesdeLasRutasPersonales() {
        // Tarea que pertenece a un grupo y está asignada al usuario
        Tarea tarea = new Tarea("Tarea de grupo", dueno);
        // La vincula al grupo
        tarea.setGrupo(grupo);
        // La consulta por responsable la devuelve
        when(tareas.findByIdAndResponsableId(TAREA, USUARIO)).thenReturn(Optional.of(tarea));

        // Editarla debe fallar por falta de permiso
        assertThatThrownBy(() -> servicio.actualizar(USUARIO, TAREA, new TareaRequest("Nuevo", null, null, null, null))).isInstanceOf(PermisoInsuficienteException.class);
        // Borrarla debe fallar por falta de permiso
        assertThatThrownBy(() -> servicio.eliminar(USUARIO, TAREA)).isInstanceOf(PermisoInsuficienteException.class);
        // Nunca se debe borrar nada
        verify(tareas, never()).delete(any(Tarea.class));
    }

    // Marca el método como prueba
    @Test
    // Quien no pertenece a un grupo no puede ver sus tareas
    void ocultaLasTareasDeUnGrupoAjeno() {
        // La consulta por grupo y usuario no encuentra nada
        when(membresias.findByGrupoIdAndUsuarioId(GRUPO, USUARIO)).thenReturn(Optional.empty());

        // Debe responder que el grupo no existe
        assertThatThrownBy(() -> servicio.listarDeGrupo(USUARIO, GRUPO, null, 0, 20)).isInstanceOf(NoEncontradoException.class);
    }

    // Marca el método como prueba
    @Test
    // Un miembro común puede crear una tarea del grupo solo para sí mismo
    void unMiembroCreaTareasDeGrupoParaSiMismo() {
        // El usuario es miembro común
        yoComo(RolGrupo.MIEMBRO);
        // El repositorio entrega la referencia al usuario
        when(usuarios.getReferenceById(USUARIO)).thenReturn(dueno);
        // El repositorio devuelve la misma tarea que recibe
        when(tareas.save(any(Tarea.class))).thenAnswer(llamada -> llamada.getArgument(0));

        // Crea una tarea sin indicar responsable
        TareaResponse respuesta = servicio.crearEnGrupo(USUARIO, GRUPO, new TareaGrupoRequest("Tarea", null, null, null, null, null));

        // La tarea quedó en el grupo
        assertThat(respuesta.grupoNombre()).isEqualTo("Orvian Team");
        // Y asignada a quien la creó
        assertThat(respuesta.responsableId()).isEqualTo(USUARIO);
    }

    // Marca el método como prueba
    @Test
    // Un miembro común no puede asignar tareas a otras personas
    void unMiembroNoAsignaTareasAOtros() {
        // El usuario es miembro común
        yoComo(RolGrupo.MIEMBRO);

        // Debe fallar por falta de permiso
        assertThatThrownBy(() -> servicio.crearEnGrupo(USUARIO, GRUPO, new TareaGrupoRequest("Tarea", null, null, null, null, OTRO))).isInstanceOf(PermisoInsuficienteException.class);
        // Nunca se debe guardar la tarea
        verify(tareas, never()).save(any(Tarea.class));
    }

    // Marca el método como prueba
    @Test
    // El líder no puede asignar una tarea a alguien que no pertenece al grupo
    void elLiderNoAsignaAPersonasAjenasAlGrupo() {
        // El usuario es líder
        yoComo(RolGrupo.LIDER);
        // La persona indicada no es integrante
        when(membresias.existsByGrupoIdAndUsuarioId(GRUPO, OTRO)).thenReturn(false);

        // Debe fallar con una solicitud inválida
        assertThatThrownBy(() -> servicio.crearEnGrupo(USUARIO, GRUPO, new TareaGrupoRequest("Tarea", null, null, null, null, OTRO))).isInstanceOf(SolicitudInvalidaException.class);
        // Nunca se debe guardar la tarea
        verify(tareas, never()).save(any(Tarea.class));
    }

    // Marca el método como prueba
    @Test
    // Un identificador de tarea de otro grupo no debe servir
    void noRevelaLasTareasDeOtroGrupo() {
        // El usuario es miembro común
        yoComo(RolGrupo.MIEMBRO);
        // La tarea no existe dentro de ese grupo
        when(tareas.findByIdAndGrupoId(TAREA, GRUPO)).thenReturn(Optional.empty());

        // Debe responder que no existe
        assertThatThrownBy(() -> servicio.registrarAvanceEnGrupo(USUARIO, GRUPO, TAREA, new AvanceRequest(EstadoTarea.HECHA, null))).isInstanceOf(NoEncontradoException.class);
    }

    // Marca el método como prueba
    @Test
    // Un miembro no puede cambiar el avance de una tarea asignada a otra persona
    void unMiembroNoCambiaElAvanceDeTareasAjenas() {
        // El usuario es miembro común
        yoComo(RolGrupo.MIEMBRO);
        // Tarea del grupo asignada a otra persona
        when(tareas.findByIdAndGrupoId(TAREA, GRUPO)).thenReturn(Optional.of(new Tarea("Tarea", otro)));

        // Debe fallar por falta de permiso
        assertThatThrownBy(() -> servicio.registrarAvanceEnGrupo(USUARIO, GRUPO, TAREA, new AvanceRequest(EstadoTarea.HECHA, null))).isInstanceOf(PermisoInsuficienteException.class);
    }

    // Marca el método como prueba
    @Test
    // La persona asignada sí puede cambiar el avance de su tarea de grupo
    void laPersonaAsignadaCambiaElAvanceDeSuTareaDeGrupo() {
        // El usuario es miembro común
        yoComo(RolGrupo.MIEMBRO);
        // Tarea del grupo asignada al usuario
        Tarea tarea = new Tarea("Tarea", dueno);
        // La vincula al grupo
        tarea.setGrupo(grupo);
        // La consulta por grupo la devuelve
        when(tareas.findByIdAndGrupoId(TAREA, GRUPO)).thenReturn(Optional.of(tarea));

        // La marca como hecha
        TareaResponse respuesta = servicio.registrarAvanceEnGrupo(USUARIO, GRUPO, TAREA, new AvanceRequest(EstadoTarea.HECHA, null));

        // El avance es 100
        assertThat(respuesta.progreso()).isEqualTo(100);
    }

    // Marca el método como prueba
    @Test
    // Solo el líder edita y borra tareas del grupo
    void soloElLiderEditaYBorraTareasDeGrupo() {
        // El usuario es miembro común
        yoComo(RolGrupo.MIEMBRO);

        // Editar debe fallar por falta de permiso
        assertThatThrownBy(() -> servicio.actualizarEnGrupo(USUARIO, GRUPO, TAREA, new TareaGrupoRequest("Nuevo", null, null, null, null, null))).isInstanceOf(PermisoInsuficienteException.class);
        // Borrar debe fallar por falta de permiso
        assertThatThrownBy(() -> servicio.eliminarEnGrupo(USUARIO, GRUPO, TAREA)).isInstanceOf(PermisoInsuficienteException.class);
        // Nunca se debe borrar nada
        verify(tareas, never()).delete(any(Tarea.class));
    }
}
