// Paquete de la capa de lógica de negocio
package com.orvian.api.service;

// Lista de elementos
import java.util.List;
// Utilidad para comparar valores que pueden ser nulos
import java.util.Objects;

// Resultado paginado de una consulta
import org.springframework.data.domain.Page;
// Anotación que marca la clase como servicio
import org.springframework.stereotype.Service;
// Anotación que ejecuta el método dentro de una transacción
import org.springframework.transaction.annotation.Transactional;

// Respuesta al registrar una descarga
import com.orvian.api.dto.DescargaResponse;
// Orden permitido para la biblioteca
import com.orvian.api.dto.OrdenRecurso;
// Datos de entrada para compartir o editar un recurso
import com.orvian.api.dto.RecursoRequest;
// Datos públicos del recurso
import com.orvian.api.dto.RecursoResponse;
// Entidad de membresía
import com.orvian.api.entity.MembresiaGrupo;
// Entidad de recurso
import com.orvian.api.entity.Recurso;
// Tipo de recurso
import com.orvian.api.entity.TipoRecurso;
// Excepción cuando un recurso no existe o es ajeno
import com.orvian.api.exception.NoEncontradoException;
// Excepción cuando falta el permiso para la acción
import com.orvian.api.exception.PermisoInsuficienteException;
// Excepción cuando se incumple una regla de negocio
import com.orvian.api.exception.SolicitudInvalidaException;
// Repositorio de recursos
import com.orvian.api.repository.RecursoRepository;
// Repositorio de usuarios
import com.orvian.api.repository.UsuarioRepository;
// Reglas comunes de paginación
import com.orvian.api.util.Paginacion;

// Lógica de negocio de la biblioteca: un recurso de grupo solo lo ven los integrantes
@Service
// Clase que gestiona los recursos compartidos
public class RecursoService {

    // Largo máximo del texto de búsqueda para no procesar entradas enormes
    private static final int TEXTO_MAXIMO = 100;

    // Acceso a los datos de recursos
    private final RecursoRepository recursos;
    // Acceso a los datos de usuarios
    private final UsuarioRepository usuarios;
    // Servicio de cursos, que valida que el curso exista
    private final CursoService cursos;
    // Reglas de acceso a los grupos
    private final AccesoGrupo acceso;

    // Spring entrega las dependencias por el constructor
    public RecursoService(RecursoRepository recursos, UsuarioRepository usuarios, CursoService cursos, AccesoGrupo acceso) {
        // Guarda el repositorio de recursos
        this.recursos = recursos;
        // Guarda el repositorio de usuarios
        this.usuarios = usuarios;
        // Guarda el servicio de cursos
        this.cursos = cursos;
        // Guarda las reglas de acceso
        this.acceso = acceso;
    }

    // Solo lee datos, así que la transacción es de solo lectura
    @Transactional(readOnly = true)
    // Busca entre los recursos que el usuario puede ver, con filtro opcional por tipo y por texto
    public Page<RecursoResponse> listar(Long usuarioId, TipoRecurso tipo, String texto, OrdenRecurso orden, int pagina, int tamano) {
        // Sin filtro de tipo se consideran todos los tipos, así la consulta nunca recibe valores nulos
        List<TipoRecurso> tipos = tipo == null ? List.of(TipoRecurso.values()) : List.of(tipo);
        // Sin texto se busca con una cadena vacía, que coincide con todos los títulos
        String buscado = texto == null ? "" : texto.trim();
        // Recorta el texto si es demasiado largo
        buscado = buscado.length() > TEXTO_MAXIMO ? buscado.substring(0, TEXTO_MAXIMO) : buscado;
        // El orden lo define el servidor según la opción elegida
        var solicitud = Paginacion.de(pagina, tamano, orden.orden());
        // Convierte cada recurso en su versión pública
        return recursos.buscarVisibles(usuarioId, tipos, buscado, solicitud).map(recurso -> RecursoResponse.desde(recurso, usuarioId));
    }

    // Solo lee datos, así que la transacción es de solo lectura
    @Transactional(readOnly = true)
    // Devuelve un recurso si el usuario puede verlo
    public RecursoResponse obtener(Long usuarioId, Long id) {
        // Busca el recurso aplicando la regla de visibilidad
        return RecursoResponse.desde(buscarVisible(usuarioId, id), usuarioId);
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Comparte un recurso; si se indica un grupo, el usuario debe pertenecer a él
    public RecursoResponse crear(Long usuarioId, RecursoRequest datos) {
        // Crea el recurso con una referencia al autor, tomada del token
        Recurso recurso = new Recurso(datos.titulo().trim(), datos.tipo(), datos.url().trim(), usuarios.getReferenceById(usuarioId));
        // Si se indicó un grupo, se exige ser integrante y se vincula
        if (datos.grupoId() != null) {
            // Exige que el usuario pertenezca al grupo
            MembresiaGrupo membresia = acceso.exigirIntegrante(usuarioId, datos.grupoId());
            // Vincula el recurso al grupo
            recurso.setGrupo(membresia.getGrupo());
        }
        // Copia los demás datos validando el curso
        aplicarDatos(recurso, datos);
        // Guarda y devuelve la versión pública
        return RecursoResponse.desde(recursos.save(recurso), usuarioId);
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Edita un recurso; solo su autor puede hacerlo
    public RecursoResponse actualizar(Long usuarioId, Long id, RecursoRequest datos) {
        // Busca el recurso aplicando la regla de visibilidad
        Recurso recurso = buscarVisible(usuarioId, id);
        // Solo el autor puede editarlo
        if (!esAutor(recurso, usuarioId)) {
            // Rechaza la acción por falta de permiso
            throw new PermisoInsuficienteException("Solo quien compartió el recurso puede editarlo");
        }
        // Cambiar el grupo permitiría publicar contenido privado, así que no se admite
        if (!Objects.equals(idDeGrupo(recurso), datos.grupoId())) {
            // Rechaza la petición con el motivo
            throw new SolicitudInvalidaException("No se puede cambiar el grupo de un recurso");
        }
        // Actualiza el título sin espacios sobrantes
        recurso.setTitulo(datos.titulo().trim());
        // Actualiza el tipo
        recurso.setTipo(datos.tipo());
        // Actualiza el enlace sin espacios sobrantes
        recurso.setUrl(datos.url().trim());
        // Copia los demás datos validando el curso
        aplicarDatos(recurso, datos);
        // Los cambios se guardan solos al terminar la transacción
        return RecursoResponse.desde(recurso, usuarioId);
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Suma una descarga y devuelve el enlace; solo si el usuario puede ver el recurso
    public DescargaResponse registrarDescarga(Long usuarioId, Long id) {
        // Busca el recurso aplicando la regla de visibilidad
        Recurso recurso = buscarVisible(usuarioId, id);
        // Suma la descarga con una consulta atómica
        recursos.incrementarDescargas(id);
        // Devuelve el enlace para que el cliente lo abra
        return new DescargaResponse(recurso.getUrl());
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Borra un recurso; lo puede hacer su autor o el líder del grupo al que pertenece
    public void eliminar(Long usuarioId, Long id) {
        // Busca el recurso aplicando la regla de visibilidad
        Recurso recurso = buscarVisible(usuarioId, id);
        // Si no es el autor, solo el líder de su grupo puede borrarlo
        if (!esAutor(recurso, usuarioId)) {
            // Un recurso público ajeno no se puede borrar
            if (recurso.getGrupo() == null) {
                // Rechaza la acción por falta de permiso
                throw new PermisoInsuficienteException("Solo quien compartió el recurso puede borrarlo");
            }
            // Exige que sea el líder del grupo del recurso
            acceso.exigirLider(usuarioId, recurso.getGrupo().getId());
        }
        // Borra el recurso
        recursos.delete(recurso);
    }

    // Un recurso que el usuario no puede ver responde igual que uno inexistente
    private Recurso buscarVisible(Long usuarioId, Long id) {
        // Consulta aplicando la regla de visibilidad
        return recursos.buscarVisiblePorId(id, usuarioId).orElseThrow(() -> new NoEncontradoException("Recurso no encontrado"));
    }

    // Indica si el usuario es quien compartió el recurso
    private boolean esAutor(Recurso recurso, Long usuarioId) {
        // Compara el autor con el usuario del token
        return recurso.getAutor().getId().equals(usuarioId);
    }

    // Identificador del grupo del recurso, o nulo si es público
    private Long idDeGrupo(Recurso recurso) {
        // Un recurso público no tiene grupo
        return recurso.getGrupo() == null ? null : recurso.getGrupo().getId();
    }

    // Copia los datos comunes de creación y edición
    private void aplicarDatos(Recurso recurso, RecursoRequest datos) {
        // Cambia la descripción
        recurso.setDescripcion(datos.descripcion());
        // Si no hay curso, el recurso queda sin curso; si lo hay, debe existir
        recurso.setCurso(cursos.buscarSiSeIndica(datos.cursoId()));
    }
}
