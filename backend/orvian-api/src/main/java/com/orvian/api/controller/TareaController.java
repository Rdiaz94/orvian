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
// Datos de entrada para crear o editar una tarea
import com.orvian.api.dto.TareaRequest;
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

// Declara el controlador REST de tareas
@RestController
// Todas las rutas de esta clase empiezan con /api/tareas
@RequestMapping("/api/tareas")
// Clase que expone las tareas personales del usuario autenticado
public class TareaController {

    // Servicio con la lógica de negocio
    private final TareaService tareaService;

    // Spring entrega el servicio por el constructor
    public TareaController(TareaService tareaService) {
        // Guarda el servicio
        this.tareaService = tareaService;
    }

    // Responde a GET /api/tareas
    @GetMapping
    // Lista las tareas del usuario; el estado y la página son opcionales
    public PagedModel<TareaResponse> listar(@AuthenticationPrincipal Jwt token, @RequestParam(required = false) EstadoTarea estado,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        // El usuario sale del token y la respuesta se envuelve en un formato JSON estable
        return new PagedModel<>(tareaService.listar(Identidad.idDe(token), estado, pagina, tamano));
    }

    // Responde a GET /api/tareas/{id}
    @GetMapping("/{id}")
    // Devuelve una tarea del usuario
    public TareaResponse obtener(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        // El usuario sale del token
        return tareaService.obtener(Identidad.idDe(token), id);
    }

    // Responde a POST /api/tareas
    @PostMapping
    // Valida los datos recibidos y crea la tarea
    public ResponseEntity<TareaResponse> crear(@AuthenticationPrincipal Jwt token, @Valid @RequestBody TareaRequest datos) {
        // Delega la lógica en el servicio
        TareaResponse creada = tareaService.crear(Identidad.idDe(token), datos);
        // Responde 201 Created con la tarea
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // Responde a PUT /api/tareas/{id}
    @PutMapping("/{id}")
    // Valida los datos recibidos y edita la tarea
    public TareaResponse actualizar(@AuthenticationPrincipal Jwt token, @PathVariable Long id, @Valid @RequestBody TareaRequest datos) {
        // El usuario sale del token
        return tareaService.actualizar(Identidad.idDe(token), id, datos);
    }

    // Responde a PATCH /api/tareas/{id}/avance
    @PatchMapping("/{id}/avance")
    // Mueve la tarea de columna o cambia su avance
    public TareaResponse registrarAvance(@AuthenticationPrincipal Jwt token, @PathVariable Long id, @Valid @RequestBody AvanceRequest datos) {
        // El usuario sale del token
        return tareaService.registrarAvance(Identidad.idDe(token), id, datos);
    }

    // Responde a DELETE /api/tareas/{id}
    @DeleteMapping("/{id}")
    // Borra la tarea y responde sin cuerpo
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        // El usuario sale del token
        tareaService.eliminar(Identidad.idDe(token), id);
        // Responde 204 No Content
        return ResponseEntity.noContent().build();
    }
}
