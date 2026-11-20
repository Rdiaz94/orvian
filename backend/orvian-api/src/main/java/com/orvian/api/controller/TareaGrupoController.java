// Paquete de los controladores de la capa de presentación
package com.orvian.api.controller;

// Constantes de los códigos de estado HTTP
import org.springframework.http.HttpStatus;
// Respuesta HTTP con estado y cuerpo
import org.springframework.http.ResponseEntity;
// Modelo de paginación con un formato JSON estable
import org.springframework.data.web.PagedModel;
// Inyecta el usuario autenticado en el método
import org.springframework.security.core.annotation.AuthenticationPrincipal;
// Token JWT ya validado
import org.springframework.security.oauth2.jwt.Jwt;
// Anotación para responder a peticiones DELETE
import org.springframework.web.bind.annotation.DeleteMapping;
// Anotación para responder a peticiones GET
import org.springframework.web.bind.annotation.GetMapping;
// Anotación que toma una parte variable de la ruta
import org.springframework.web.bind.annotation.PathVariable;
// Anotación para responder a peticiones PATCH
import org.springframework.web.bind.annotation.PatchMapping;
// Anotación para recibir datos en una petición POST
import org.springframework.web.bind.annotation.PostMapping;
// Anotación para responder a peticiones PUT
import org.springframework.web.bind.annotation.PutMapping;
// Anotación que toma el cuerpo JSON de la petición
import org.springframework.web.bind.annotation.RequestBody;
// Anotación que agrupa las rutas bajo un prefijo común
import org.springframework.web.bind.annotation.RequestMapping;
// Anotación que toma un parámetro de la URL
import org.springframework.web.bind.annotation.RequestParam;
// Anotación que indica que la clase responde con JSON
import org.springframework.web.bind.annotation.RestController;

// Datos de entrada para mover o avanzar una tarea
import com.orvian.api.dto.AvanceRequest;
// Datos de entrada para crear o editar una tarea de grupo
import com.orvian.api.dto.TareaGrupoRequest;
// Datos públicos de la tarea
import com.orvian.api.dto.TareaResponse;
// Estado de la tarea
import com.orvian.api.entity.EstadoTarea;
// Obtiene quién hace la petición
import com.orvian.api.security.Identidad;
// Servicio de tareas
import com.orvian.api.service.TareaService;
// Anotación que activa la validación del cuerpo recibido
import jakarta.validation.Valid;

// Declara el controlador REST de las tareas de un grupo
@RestController
// Todas las rutas de esta clase empiezan con /api/grupos/{grupoId}/tareas
@RequestMapping("/api/grupos/{grupoId}/tareas")
// Clase que expone las tareas compartidas de un grupo a sus integrantes
public class TareaGrupoController {

    // Servicio con la lógica de negocio
    private final TareaService tareaService;

    // Spring entrega el servicio por el constructor
    public TareaGrupoController(TareaService tareaService) {
        // Guarda el servicio
        this.tareaService = tareaService;
    }

    // Responde a GET /api/grupos/{grupoId}/tareas
    @GetMapping
    // Lista las tareas del grupo; el estado y la página son opcionales
    public PagedModel<TareaResponse> listar(@AuthenticationPrincipal Jwt token, @PathVariable Long grupoId, @RequestParam(required = false) EstadoTarea estado,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        // El usuario sale del token y la respuesta se envuelve en un formato JSON estable
        return new PagedModel<>(tareaService.listarDeGrupo(Identidad.idDe(token), grupoId, estado, pagina, tamano));
    }

    // Responde a POST /api/grupos/{grupoId}/tareas
    @PostMapping
    // Valida los datos recibidos y crea la tarea en el grupo
    public ResponseEntity<TareaResponse> crear(@AuthenticationPrincipal Jwt token, @PathVariable Long grupoId, @Valid @RequestBody TareaGrupoRequest datos) {
        // Delega la lógica en el servicio
        TareaResponse creada = tareaService.crearEnGrupo(Identidad.idDe(token), grupoId, datos);
        // Responde 201 Created con la tarea
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // Responde a PUT /api/grupos/{grupoId}/tareas/{tareaId}
    @PutMapping("/{tareaId}")
    // Valida los datos recibidos y edita la tarea; solo el líder
    public TareaResponse actualizar(@AuthenticationPrincipal Jwt token, @PathVariable Long grupoId, @PathVariable Long tareaId, @Valid @RequestBody TareaGrupoRequest datos) {
        // El usuario sale del token
        return tareaService.actualizarEnGrupo(Identidad.idDe(token), grupoId, tareaId, datos);
    }

    // Responde a PATCH /api/grupos/{grupoId}/tareas/{tareaId}/avance
    @PatchMapping("/{tareaId}/avance")
    // Cambia el estado y el avance; la persona asignada o el líder
    public TareaResponse registrarAvance(@AuthenticationPrincipal Jwt token, @PathVariable Long grupoId, @PathVariable Long tareaId, @Valid @RequestBody AvanceRequest datos) {
        // El usuario sale del token
        return tareaService.registrarAvanceEnGrupo(Identidad.idDe(token), grupoId, tareaId, datos);
    }

    // Responde a DELETE /api/grupos/{grupoId}/tareas/{tareaId}
    @DeleteMapping("/{tareaId}")
    // Borra la tarea y responde sin cuerpo; solo el líder
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Jwt token, @PathVariable Long grupoId, @PathVariable Long tareaId) {
        // El usuario sale del token
        tareaService.eliminarEnGrupo(Identidad.idDe(token), grupoId, tareaId);
        // Responde 204 No Content
        return ResponseEntity.noContent().build();
    }
}
