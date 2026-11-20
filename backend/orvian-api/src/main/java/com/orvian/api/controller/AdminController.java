// Paquete de los controladores de la capa de presentación
package com.orvian.api.controller;

// Constantes de los códigos de estado HTTP
import org.springframework.http.HttpStatus;
// Respuesta HTTP con estado y cuerpo
import org.springframework.http.ResponseEntity;
// Modelo de paginación con un formato JSON estable
import org.springframework.data.web.PagedModel;
// Anotación para responder a peticiones GET
import org.springframework.web.bind.annotation.GetMapping;
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

// Datos de entrada para crear un curso
import com.orvian.api.dto.CursoRequest;
// Datos públicos del curso
import com.orvian.api.dto.CursoResponse;
// Datos públicos del usuario
import com.orvian.api.dto.UsuarioResponse;
// Servicio de cursos
import com.orvian.api.service.CursoService;
// Servicio de usuarios
import com.orvian.api.service.UsuarioService;
// Anotación que activa la validación del cuerpo recibido
import jakarta.validation.Valid;

// Declara el controlador REST de administración
@RestController
// Todas las rutas de esta clase empiezan con /api/admin y exigen el rol ADMIN
@RequestMapping("/api/admin")
// Clase que expone operaciones reservadas a los administradores
public class AdminController {

    // Servicio de usuarios
    private final UsuarioService usuarioService;
    // Servicio de cursos
    private final CursoService cursoService;

    // Spring entrega los servicios por el constructor
    public AdminController(UsuarioService usuarioService, CursoService cursoService) {
        // Guarda el servicio de usuarios
        this.usuarioService = usuarioService;
        // Guarda el servicio de cursos
        this.cursoService = cursoService;
    }

    // Responde a GET /api/admin/usuarios
    @GetMapping("/usuarios")
    // Devuelve una página de usuarios; los parámetros son opcionales
    public PagedModel<UsuarioResponse> listarUsuarios(@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        // Delega en el servicio y envuelve el resultado en un formato JSON estable
        return new PagedModel<>(usuarioService.listar(pagina, tamano));
    }

    // Responde a POST /api/admin/cursos
    @PostMapping("/cursos")
    // Valida los datos recibidos antes de ejecutar el método
    public ResponseEntity<CursoResponse> crearCurso(@Valid @RequestBody CursoRequest datos) {
        // Delega la lógica en el servicio
        CursoResponse creado = cursoService.crear(datos);
        // Responde 201 Created con los datos del curso
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
}
