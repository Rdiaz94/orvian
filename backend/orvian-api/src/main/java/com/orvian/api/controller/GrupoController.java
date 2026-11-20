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
// Anotación para recibir datos en una petición POST
import org.springframework.web.bind.annotation.PostMapping;
// Anotación que toma el cuerpo JSON de la petición
import org.springframework.web.bind.annotation.RequestBody;
// Anotación que agrupa las rutas bajo un prefijo común
import org.springframework.web.bind.annotation.RequestMapping;
// Anotación que toma un parámetro de la URL
import org.springframework.web.bind.annotation.RequestParam;
// Anotación que indica que la clase responde con JSON
import org.springframework.web.bind.annotation.RestController;

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
// Obtiene quién hace la petición
import com.orvian.api.security.Identidad;
// Servicio de grupos
import com.orvian.api.service.GrupoService;
// Anotación que activa la validación del cuerpo recibido
import jakarta.validation.Valid;

// Declara el controlador REST de grupos
@RestController
// Todas las rutas de esta clase empiezan con /api/grupos
@RequestMapping("/api/grupos")
// Clase que expone los grupos de trabajo del usuario autenticado
public class GrupoController {

    // Servicio con la lógica de negocio
    private final GrupoService grupoService;

    // Spring entrega el servicio por el constructor
    public GrupoController(GrupoService grupoService) {
        // Guarda el servicio
        this.grupoService = grupoService;
    }

    // Responde a GET /api/grupos
    @GetMapping
    // Lista los grupos del usuario; la página es opcional
    public PagedModel<GrupoResponse> listar(@AuthenticationPrincipal Jwt token, @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        // El usuario sale del token y la respuesta se envuelve en un formato JSON estable
        return new PagedModel<>(grupoService.listar(Identidad.idDe(token), pagina, tamano));
    }

    // Responde a POST /api/grupos
    @PostMapping
    // Valida los datos recibidos y crea el grupo
    public ResponseEntity<GrupoResponse> crear(@AuthenticationPrincipal Jwt token, @Valid @RequestBody GrupoRequest datos) {
        // Delega la lógica en el servicio
        GrupoResponse creado = grupoService.crear(Identidad.idDe(token), datos);
        // Responde 201 Created con el grupo
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // Responde a GET /api/grupos/{id}
    @GetMapping("/{id}")
    // Devuelve el grupo con sus integrantes si el usuario pertenece a él
    public GrupoDetalleResponse obtener(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        // El usuario sale del token
        return grupoService.obtener(Identidad.idDe(token), id);
    }

    // Responde a POST /api/grupos/{id}/integrantes
    @PostMapping("/{id}/integrantes")
    // Valida los datos recibidos y suma a la persona al grupo
    public ResponseEntity<IntegranteResponse> agregarIntegrante(@AuthenticationPrincipal Jwt token, @PathVariable Long id, @Valid @RequestBody IntegranteRequest datos) {
        // Delega la lógica en el servicio
        IntegranteResponse nuevo = grupoService.agregarIntegrante(Identidad.idDe(token), id, datos);
        // Responde 201 Created con el integrante
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    // Responde a DELETE /api/grupos/{id}/integrantes/{integranteId}
    @DeleteMapping("/{id}/integrantes/{integranteId}")
    // Quita a un integrante o permite que la persona abandone el grupo
    public ResponseEntity<Void> quitarIntegrante(@AuthenticationPrincipal Jwt token, @PathVariable Long id, @PathVariable Long integranteId) {
        // El usuario sale del token
        grupoService.quitarIntegrante(Identidad.idDe(token), id, integranteId);
        // Responde 204 No Content
        return ResponseEntity.noContent().build();
    }

    // Responde a DELETE /api/grupos/{id}
    @DeleteMapping("/{id}")
    // Elimina el grupo y responde sin cuerpo
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        // El usuario sale del token
        grupoService.eliminar(Identidad.idDe(token), id);
        // Responde 204 No Content
        return ResponseEntity.noContent().build();
    }
}
