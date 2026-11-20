// Paquete de la capa de lógica de negocio
package com.orvian.api.service;

// Lista de resultados
import java.util.List;

// Excepción de Spring cuando se viola una restricción de la base de datos
import org.springframework.dao.DataIntegrityViolationException;
// Resultado paginado de una consulta
import org.springframework.data.domain.Page;
// Define el orden de una consulta
import org.springframework.data.domain.Sort;
// Anotación que marca la clase como servicio
import org.springframework.stereotype.Service;
// Anotación que ejecuta el método dentro de una transacción
import org.springframework.transaction.annotation.Transactional;

// Datos de entrada para crear un grupo
import com.orvian.api.dto.GrupoRequest;
// Datos públicos del grupo con sus integrantes
import com.orvian.api.dto.GrupoDetalleResponse;
// Datos públicos del grupo
import com.orvian.api.dto.GrupoResponse;
// Datos de entrada para invitar a una persona
import com.orvian.api.dto.IntegranteRequest;
// Datos públicos de un integrante
import com.orvian.api.dto.IntegranteResponse;
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
// Reglas comunes para tratar correos
import com.orvian.api.util.Correos;
// Reglas comunes de paginación
import com.orvian.api.util.Paginacion;

// Lógica de negocio de los grupos: solo los integrantes ven un grupo y solo el líder lo administra
@Service
// Clase que gestiona grupos de trabajo e integrantes
public class GrupoService {

    // Acceso a los datos de grupos
    private final GrupoRepository grupos;
    // Acceso a los datos de membresías
    private final MembresiaGrupoRepository membresias;
    // Acceso a los datos de usuarios
    private final UsuarioRepository usuarios;
    // Servicio de cursos, que valida que el curso exista
    private final CursoService cursos;
    // Reglas de acceso a los grupos
    private final AccesoGrupo acceso;
    // Acceso a los datos de tareas
    private final TareaRepository tareas;
    // Acceso a los datos de recursos
    private final RecursoRepository recursos;

    // Spring entrega las dependencias por el constructor
    public GrupoService(GrupoRepository grupos, MembresiaGrupoRepository membresias, UsuarioRepository usuarios, CursoService cursos,
            AccesoGrupo acceso, TareaRepository tareas, RecursoRepository recursos) {
        // Guarda el repositorio de grupos
        this.grupos = grupos;
        // Guarda el repositorio de membresías
        this.membresias = membresias;
        // Guarda el repositorio de usuarios
        this.usuarios = usuarios;
        // Guarda el servicio de cursos
        this.cursos = cursos;
        // Guarda las reglas de acceso
        this.acceso = acceso;
        // Guarda el repositorio de tareas
        this.tareas = tareas;
        // Guarda el repositorio de recursos
        this.recursos = recursos;
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Crea un grupo y convierte a su creador en líder
    public GrupoResponse crear(Long usuarioId, GrupoRequest datos) {
        // Referencia al creador, tomada del token
        Usuario creador = usuarios.getReferenceById(usuarioId);
        // Crea el grupo con el nombre sin espacios sobrantes
        Grupo grupo = new Grupo(datos.nombre().trim(), creador);
        // Asigna la descripción
        grupo.setDescripcion(datos.descripcion());
        // Asigna el curso si se indicó, validando que exista
        grupo.setCurso(cursos.buscarSiSeIndica(datos.cursoId()));
        // Guarda el grupo
        grupos.save(grupo);
        // El creador queda registrado como líder en la misma transacción
        membresias.save(new MembresiaGrupo(grupo, creador, RolGrupo.LIDER));
        // Devuelve el grupo desde el punto de vista del líder
        return GrupoResponse.desde(grupo, RolGrupo.LIDER);
    }

    // Solo lee datos, así que la transacción es de solo lectura
    @Transactional(readOnly = true)
    // Lista los grupos a los que pertenece el usuario
    public Page<GrupoResponse> listar(Long usuarioId, int pagina, int tamano) {
        // Orden fijo: primero los grupos a los que ingresó más recientemente
        var solicitud = Paginacion.de(pagina, tamano, Sort.by("fechaIngreso").descending().and(Sort.by("id")));
        // Cada membresía aporta el grupo y el papel del usuario
        return membresias.findByUsuarioId(usuarioId, solicitud).map(membresia -> GrupoResponse.desde(membresia.getGrupo(), membresia.getRolGrupo()));
    }

    // Solo lee datos, así que la transacción es de solo lectura
    @Transactional(readOnly = true)
    // Devuelve el grupo con sus integrantes si el usuario pertenece a él
    public GrupoDetalleResponse obtener(Long usuarioId, Long grupoId) {
        // Exige que el usuario sea integrante
        MembresiaGrupo propia = acceso.exigirIntegrante(usuarioId, grupoId);
        // Convierte los integrantes en su versión pública
        List<IntegranteResponse> integrantes = membresias.findByGrupoId(grupoId).stream().map(IntegranteResponse::desde).toList();
        // Devuelve el grupo con sus integrantes
        return new GrupoDetalleResponse(GrupoResponse.desde(propia.getGrupo(), propia.getRolGrupo()), integrantes);
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Suma a una persona al grupo; solo lo puede hacer el líder
    public IntegranteResponse agregarIntegrante(Long usuarioId, Long grupoId, IntegranteRequest datos) {
        // Exige que el usuario sea el líder
        MembresiaGrupo lider = acceso.exigirLider(usuarioId, grupoId);
        // Busca a la persona por su correo normalizado
        Usuario nuevo = usuarios.findByCorreo(Correos.normalizar(datos.correo()))
                .orElseThrow(() -> new NoEncontradoException("No existe un usuario con ese correo"));
        // Si ya es integrante, se rechaza
        if (membresias.existsByGrupoIdAndUsuarioId(grupoId, nuevo.getId())) {
            // Lanza el conflicto
            throw integranteRepetido();
        }
        // Intenta guardar la membresía
        try {
            // Guarda a la persona como miembro y devuelve su versión pública
            return IntegranteResponse.desde(membresias.save(new MembresiaGrupo(lider.getGrupo(), nuevo, RolGrupo.MIEMBRO)));
        // Si dos invitaciones simultáneas chocan, la restricción única de la base de datos lo detecta
        } catch (DataIntegrityViolationException error) {
            // Se trata igual que un integrante repetido
            throw integranteRepetido();
        }
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // El líder quita a un integrante, o cualquier integrante abandona el grupo por su cuenta
    public void quitarIntegrante(Long usuarioId, Long grupoId, Long integranteId) {
        // Exige que quien actúa sea integrante
        MembresiaGrupo propia = acceso.exigirIntegrante(usuarioId, grupoId);
        // Indica si la persona se está yendo por sí misma
        boolean abandona = integranteId.equals(usuarioId);
        // Solo el líder puede quitar a otra persona
        if (!abandona && propia.getRolGrupo() != RolGrupo.LIDER) {
            // Rechaza la acción por falta de permiso
            throw new PermisoInsuficienteException("Solo el líder puede quitar integrantes");
        }
        // El líder no puede irse: el grupo quedaría sin responsable
        if (abandona && propia.getRolGrupo() == RolGrupo.LIDER) {
            // Rechaza la acción con el motivo
            throw new SolicitudInvalidaException("El líder no puede abandonar el grupo; elimínalo si ya no se necesita");
        }
        // Si se va por su cuenta, se borra su propia membresía; si no, se busca la del otro integrante
        MembresiaGrupo objetivo = abandona ? propia
                : membresias.findByGrupoIdAndUsuarioId(grupoId, integranteId).orElseThrow(() -> new NoEncontradoException("Integrante no encontrado"));
        // Borra la membresía
        membresias.delete(objetivo);
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Elimina el grupo; solo lo puede hacer el líder
    public void eliminar(Long usuarioId, Long grupoId) {
        // Exige que el usuario sea el líder
        MembresiaGrupo lider = acceso.exigirLider(usuarioId, grupoId);
        // Borra las tareas del grupo para no dejar datos huérfanos
        tareas.deleteByGrupoId(grupoId);
        // Borra los recursos del grupo: no se vuelven públicos para no exponer contenido privado
        recursos.deleteByGrupoId(grupoId);
        // Borra las membresías porque dependen del grupo
        membresias.deleteByGrupoId(grupoId);
        // Borra el grupo
        grupos.delete(lider.getGrupo());
    }

    // Construye el conflicto en un solo lugar para no repetir el mensaje
    private ConflictoException integranteRepetido() {
        // Mensaje claro para el líder
        return new ConflictoException("Esa persona ya es integrante del grupo");
    }
}
