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

// Respuesta al registrar una descarga
import com.orvian.api.dto.DescargaResponse;
// Orden permitido para la biblioteca
import com.orvian.api.dto.OrdenRecurso;
// Datos de entrada para compartir o editar un recurso
import com.orvian.api.dto.RecursoRequest;
// Datos públicos del recurso
import com.orvian.api.dto.RecursoResponse;
// Tipo de recurso
import com.orvian.api.entity.TipoRecurso;
// Obtiene quién hace la petición
import com.orvian.api.security.Identidad;
// Servicio de recursos
import com.orvian.api.service.RecursoService;
// Anotación que activa la validación del cuerpo recibido
import jakarta.validation.Valid;

// Declara el controlador REST de la biblioteca de recursos
@RestController
// Todas las rutas de esta clase empiezan con /api/recursos
@RequestMapping("/api/recursos")
// Clase que expone la biblioteca a los usuarios autenticados
public class RecursoController {

    // Servicio con la lógica de negocio
    private final RecursoService recursoService;

    // Spring entrega el servicio por el constructor
    public RecursoController(RecursoService recursoService) {
        // Guarda el servicio
        this.recursoService = recursoService;
    }

    // Responde a GET /api/recursos
    @GetMapping
    // Busca recursos; todos los parámetros son opcionales
    public PagedModel<RecursoResponse> listar(@AuthenticationPrincipal Jwt token, @RequestParam(required = false) TipoRecurso tipo,
            @RequestParam(name = "q", required = false) String texto, @RequestParam(defaultValue = "RECIENTES") OrdenRecurso orden,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        // El usuario sale del token y la respuesta se envuelve en un formato JSON estable
        return new PagedModel<>(recursoService.listar(Identidad.idDe(token), tipo, texto, orden, pagina, tamano));
    }

    // Responde a GET /api/recursos/{id}
    @GetMapping("/{id}")
    // Devuelve un recurso si el usuario puede verlo
    public RecursoResponse obtener(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        // El usuario sale del token
        return recursoService.obtener(Identidad.idDe(token), id);
    }

    // Responde a POST /api/recursos
    @PostMapping
    // Valida los datos recibidos y comparte el recurso
    public ResponseEntity<RecursoResponse> crear(@AuthenticationPrincipal Jwt token, @Valid @RequestBody RecursoRequest datos) {
        // Delega la lógica en el servicio
        RecursoResponse creado = recursoService.crear(Identidad.idDe(token), datos);
        // Responde 201 Created con el recurso
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // Responde a PUT /api/recursos/{id}
    @PutMapping("/{id}")
    // Valida los datos recibidos y edita el recurso; solo su autor
    public RecursoResponse actualizar(@AuthenticationPrincipal Jwt token, @PathVariable Long id, @Valid @RequestBody RecursoRequest datos) {
        // El usuario sale del token
        return recursoService.actualizar(Identidad.idDe(token), id, datos);
    }

    // Responde a POST /api/recursos/{id}/descarga
    @PostMapping("/{id}/descarga")
    // Suma una descarga y devuelve el enlace
    public DescargaResponse descargar(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        // El usuario sale del token
        return recursoService.registrarDescarga(Identidad.idDe(token), id);
    }

    // Responde a DELETE /api/recursos/{id}
    @DeleteMapping("/{id}")
    // Borra el recurso y responde sin cuerpo
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        // El usuario sale del token
        recursoService.eliminar(Identidad.idDe(token), id);
        // Responde 204 No Content
        return ResponseEntity.noContent().build();
    }
}
