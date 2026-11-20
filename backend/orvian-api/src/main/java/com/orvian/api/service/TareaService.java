// Paquete de la capa de lógica de negocio
package com.orvian.api.service;

// Resultado paginado de una consulta
import org.springframework.data.domain.Page;
// Crea una solicitud de página
import org.springframework.data.domain.PageRequest;
// Define el orden de una consulta
import org.springframework.data.domain.Sort;
// Anotación que marca la clase como servicio
import org.springframework.stereotype.Service;
// Anotación que ejecuta el método dentro de una transacción
import org.springframework.transaction.annotation.Transactional;

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
// Entidad de membresía
import com.orvian.api.entity.MembresiaGrupo;
// Papel dentro del grupo
import com.orvian.api.entity.RolGrupo;
// Entidad de tarea
import com.orvian.api.entity.Tarea;
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
// Reglas comunes de paginación
import com.orvian.api.util.Paginacion;

// Lógica de negocio de las tareas: el usuario siempre llega desde su token, nunca desde la petición
@Service
// Clase que gestiona las tareas personales y las tareas de grupo
public class TareaService {

    // Acceso a los datos de tareas
    private final TareaRepository tareas;
    // Acceso a los datos de usuarios
    private final UsuarioRepository usuarios;
    // Servicio de cursos, que valida que el curso exista
    private final CursoService cursos;
    // Acceso a los datos de membresías
    private final MembresiaGrupoRepository membresias;
    // Reglas de acceso a los grupos
    private final AccesoGrupo acceso;

    // Spring entrega las dependencias por el constructor
    public TareaService(TareaRepository tareas, UsuarioRepository usuarios, CursoService cursos, MembresiaGrupoRepository membresias, AccesoGrupo acceso) {
        // Guarda el repositorio de tareas
        this.tareas = tareas;
        // Guarda el repositorio de usuarios
        this.usuarios = usuarios;
        // Guarda el servicio de cursos
        this.cursos = cursos;
        // Guarda el repositorio de membresías
        this.membresias = membresias;
        // Guarda las reglas de acceso
        this.acceso = acceso;
    }

    // ===== Tareas personales: las que están asignadas al usuario =====

    // Solo lee datos, así que la transacción es de solo lectura
    @Transactional(readOnly = true)
    // Lista las tareas asignadas al usuario, con filtro opcional por estado
    public Page<TareaResponse> listar(Long usuarioId, EstadoTarea estado, int pagina, int tamano) {
        // Orden fijo definido por el servidor
        PageRequest solicitud = ordenadaPorVencimiento(pagina, tamano);
        // Sin filtro, trae todas las tareas del usuario; con filtro, solo las de ese estado
        Page<Tarea> resultado = estado == null
                ? tareas.findByResponsableId(usuarioId, solicitud)
                : tareas.findByResponsableIdAndEstado(usuarioId, estado, solicitud);
        // Convierte cada tarea en su versión pública
        return resultado.map(TareaResponse::desde);
    }

    // Solo lee datos, así que la transacción es de solo lectura
    @Transactional(readOnly = true)
    // Devuelve una tarea asignada al usuario
    public TareaResponse obtener(Long usuarioId, Long id) {
        // Busca la tarea solo entre las del usuario y la convierte
        return TareaResponse.desde(buscarPropia(usuarioId, id));
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Crea una tarea personal para el usuario autenticado
    public TareaResponse crear(Long usuarioId, TareaRequest datos) {
        // Crea la tarea con una referencia al usuario, sin consultarlo a la base de datos
        Tarea tarea = new Tarea(datos.titulo().trim(), usuarios.getReferenceById(usuarioId));
        // Copia los demás datos validando el curso
        aplicarDatos(tarea, datos);
        // Guarda y devuelve la versión pública
        return TareaResponse.desde(tareas.save(tarea));
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Edita los datos de una tarea personal
    public TareaResponse actualizar(Long usuarioId, Long id, TareaRequest datos) {
        // Busca la tarea y exige que sea personal
        Tarea tarea = exigirPersonal(buscarPropia(usuarioId, id));
        // Actualiza el título sin espacios sobrantes
        tarea.setTitulo(datos.titulo().trim());
        // Copia los demás datos validando el curso
        aplicarDatos(tarea, datos);
        // Los cambios se guardan solos al terminar la transacción
        return TareaResponse.desde(tarea);
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Cambia el estado y el avance de una tarea asignada al usuario, personal o de grupo
    public TareaResponse registrarAvance(Long usuarioId, Long id, AvanceRequest datos) {
        // Busca la tarea solo entre las asignadas al usuario
        Tarea tarea = buscarPropia(usuarioId, id);
        // Aplica el cambio manteniendo coherentes el estado y el avance
        aplicarAvance(tarea, datos);
        // Los cambios se guardan solos al terminar la transacción
        return TareaResponse.desde(tarea);
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Borra una tarea personal
    public void eliminar(Long usuarioId, Long id) {
        // Busca la tarea y exige que sea personal; las de grupo las borra el líder
        tareas.delete(exigirPersonal(buscarPropia(usuarioId, id)));
    }

    // ===== Tareas de grupo: las administra el líder y las ven todos los integrantes =====

    // Solo lee datos, así que la transacción es de solo lectura
    @Transactional(readOnly = true)
    // Lista las tareas de un grupo; cualquier integrante puede verlas
    public Page<TareaResponse> listarDeGrupo(Long usuarioId, Long grupoId, EstadoTarea estado, int pagina, int tamano) {
        // Exige que el usuario pertenezca al grupo
        acceso.exigirIntegrante(usuarioId, grupoId);
        // Orden fijo definido por el servidor
        PageRequest solicitud = ordenadaPorVencimiento(pagina, tamano);
        // Sin filtro, trae todas las tareas del grupo; con filtro, solo las de ese estado
        Page<Tarea> resultado = estado == null
                ? tareas.findByGrupoId(grupoId, solicitud)
                : tareas.findByGrupoIdAndEstado(grupoId, estado, solicitud);
        // Convierte cada tarea en su versión pública
        return resultado.map(TareaResponse::desde);
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Crea una tarea en el grupo; un miembro la crea para sí y el líder puede asignarla a otra persona
    public TareaResponse crearEnGrupo(Long usuarioId, Long grupoId, TareaGrupoRequest datos) {
        // Exige que el usuario pertenezca al grupo
        MembresiaGrupo propia = acceso.exigirIntegrante(usuarioId, grupoId);
        // Si no se indica responsable, la tarea queda para quien la crea
        Long responsableId = datos.responsableId() == null ? usuarioId : datos.responsableId();
        // Valida que la asignación esté permitida
        validarAsignacion(propia, grupoId, responsableId);
        // Crea la tarea con una referencia al responsable
        Tarea tarea = new Tarea(datos.titulo().trim(), usuarios.getReferenceById(responsableId));
        // La tarea pertenece al grupo del que se verificó la membresía
        tarea.setGrupo(propia.getGrupo());
        // Copia los demás datos validando el curso
        aplicarDatos(tarea, datos.datosDeTarea());
        // Guarda y devuelve la versión pública
        return TareaResponse.desde(tareas.save(tarea));
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Edita una tarea del grupo; solo el líder puede hacerlo
    public TareaResponse actualizarEnGrupo(Long usuarioId, Long grupoId, Long tareaId, TareaGrupoRequest datos) {
        // Exige que el usuario sea el líder
        MembresiaGrupo lider = acceso.exigirLider(usuarioId, grupoId);
        // Busca la tarea solo dentro de ese grupo
        Tarea tarea = buscarDeGrupo(grupoId, tareaId);
        // Actualiza el título sin espacios sobrantes
        tarea.setTitulo(datos.titulo().trim());
        // Copia los demás datos validando el curso
        aplicarDatos(tarea, datos.datosDeTarea());
        // Si se indicó otro responsable, se valida y se reasigna
        if (datos.responsableId() != null) {
            // Valida que la asignación esté permitida
            validarAsignacion(lider, grupoId, datos.responsableId());
            // Cambia el responsable
            tarea.setResponsable(usuarios.getReferenceById(datos.responsableId()));
        }
        // Los cambios se guardan solos al terminar la transacción
        return TareaResponse.desde(tarea);
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Cambia el avance de una tarea del grupo; lo puede hacer la persona asignada o el líder
    public TareaResponse registrarAvanceEnGrupo(Long usuarioId, Long grupoId, Long tareaId, AvanceRequest datos) {
        // Exige que el usuario pertenezca al grupo
        MembresiaGrupo propia = acceso.exigirIntegrante(usuarioId, grupoId);
        // Busca la tarea solo dentro de ese grupo
        Tarea tarea = buscarDeGrupo(grupoId, tareaId);
        // Indica si la tarea está asignada al usuario
        boolean esResponsable = tarea.getResponsable().getId().equals(usuarioId);
        // Si no es la persona asignada ni el líder, no tiene permiso
        if (!esResponsable && propia.getRolGrupo() != RolGrupo.LIDER) {
            // Rechaza la acción por falta de permiso
            throw new PermisoInsuficienteException("Solo la persona asignada o el líder pueden cambiar el avance");
        }
        // Aplica el cambio manteniendo coherentes el estado y el avance
        aplicarAvance(tarea, datos);
        // Los cambios se guardan solos al terminar la transacción
        return TareaResponse.desde(tarea);
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Borra una tarea del grupo; solo el líder puede hacerlo
    public void eliminarEnGrupo(Long usuarioId, Long grupoId, Long tareaId) {
        // Exige que el usuario sea el líder
        acceso.exigirLider(usuarioId, grupoId);
        // Busca la tarea solo dentro de ese grupo y la borra
        tareas.delete(buscarDeGrupo(grupoId, tareaId));
    }

    // ===== Piezas compartidas =====

    // Orden fijo: primero las que vencen antes; las que no tienen fecha quedan al final
    private PageRequest ordenadaPorVencimiento(int pagina, int tamano) {
        // Aplica las reglas comunes de paginación
        return Paginacion.de(pagina, tamano, Sort.by("fechaVencimiento").and(Sort.by("id")));
    }

    // Busca una tarea solo si está asignada al usuario; así nunca se accede a tareas ajenas
    private Tarea buscarPropia(Long usuarioId, Long id) {
        // Consulta por identificador y responsable a la vez
        return tareas.findByIdAndResponsableId(id, usuarioId).orElseThrow(TareaService::tareaNoEncontrada);
    }

    // Busca una tarea solo dentro del grupo indicado; un identificador de otro grupo no sirve
    private Tarea buscarDeGrupo(Long grupoId, Long tareaId) {
        // Consulta por identificador y grupo a la vez
        return tareas.findByIdAndGrupoId(tareaId, grupoId).orElseThrow(TareaService::tareaNoEncontrada);
    }

    // Las tareas de grupo no se editan ni se borran desde las rutas personales
    private Tarea exigirPersonal(Tarea tarea) {
        // Si pertenece a un grupo, su gestión corresponde al líder
        if (tarea.getGrupo() != null) {
            // Rechaza la acción por falta de permiso
            throw new PermisoInsuficienteException("Las tareas de grupo las administra el líder desde el grupo");
        }
        // Devuelve la tarea personal
        return tarea;
    }

    // Solo el líder puede asignar a otra persona, y esa persona debe pertenecer al grupo
    private void validarAsignacion(MembresiaGrupo quienAsigna, Long grupoId, Long responsableId) {
        // Asignarse a uno mismo siempre está permitido
        if (responsableId.equals(quienAsigna.getUsuario().getId())) {
            // No hay nada más que validar
            return;
        }
        // Asignar a otra persona es una facultad del líder
        if (quienAsigna.getRolGrupo() != RolGrupo.LIDER) {
            // Rechaza la acción por falta de permiso
            throw new PermisoInsuficienteException("Solo el líder puede asignar tareas a otras personas");
        }
        // La persona asignada debe ser integrante del grupo
        if (!membresias.existsByGrupoIdAndUsuarioId(grupoId, responsableId)) {
            // Rechaza la petición con el motivo
            throw new SolicitudInvalidaException("La persona asignada no pertenece al grupo");
        }
    }

    // Copia los datos comunes de creación y edición
    private void aplicarDatos(Tarea tarea, TareaRequest datos) {
        // Cambia la descripción
        tarea.setDescripcion(datos.descripcion());
        // Si no se indica etiqueta, se usa OTRO
        tarea.setEtiqueta(datos.etiqueta() == null ? EtiquetaTarea.OTRO : datos.etiqueta());
        // Cambia la fecha límite
        tarea.setFechaVencimiento(datos.fechaVencimiento());
        // Si no hay curso, la tarea queda sin curso; si lo hay, debe existir
        tarea.setCurso(cursos.buscarSiSeIndica(datos.cursoId()));
    }

    // El avance depende del estado para que nunca queden datos contradictorios
    private void aplicarAvance(Tarea tarea, AvanceRequest datos) {
        // Elige la regla según el nuevo estado
        switch (datos.estado()) {
            // Una tarea pendiente no tiene avance
            case PENDIENTE -> tarea.setProgreso(0);
            // Una tarea hecha está al 100 %
            case HECHA -> tarea.setProgreso(100);
            // Una tarea en progreso necesita un avance intermedio
            case EN_PROGRESO -> {
                // Avance enviado por el cliente
                Integer avance = datos.progreso();
                // Debe existir y estar entre 1 y 99
                if (avance == null || avance < 1 || avance > 99) {
                    // Rechaza la petición con el motivo
                    throw new SolicitudInvalidaException("Una tarea en progreso necesita un avance entre 1 y 99");
                }
                // Guarda el avance
                tarea.setProgreso(avance);
            }
        }
        // Guarda el nuevo estado
        tarea.setEstado(datos.estado());
    }

    // Construye el error en un solo lugar: es el mismo para "no existe" y "no es tuya"
    private static NoEncontradoException tareaNoEncontrada() {
        // Mensaje genérico que no revela si la tarea existe
        return new NoEncontradoException("Tarea no encontrada");
    }
}
