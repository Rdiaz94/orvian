// Paquete de los controladores de la capa de presentación
package com.orvian.api.controller;

// Lista de resultados
import java.util.List;

// Anotación para responder a peticiones GET
import org.springframework.web.bind.annotation.GetMapping;
// Anotación que agrupa las rutas bajo un prefijo común
import org.springframework.web.bind.annotation.RequestMapping;
// Anotación que indica que la clase responde con JSON
import org.springframework.web.bind.annotation.RestController;

// Datos públicos del curso
import com.orvian.api.dto.CursoResponse;
// Servicio de cursos
import com.orvian.api.service.CursoService;

// Declara el controlador REST de cursos
@RestController
// Todas las rutas de esta clase empiezan con /api/cursos
@RequestMapping("/api/cursos")
// Clase que expone la consulta de cursos a cualquier usuario autenticado
public class CursoController {

    // Servicio con la lógica de negocio
    private final CursoService cursoService;

    // Spring entrega el servicio por el constructor
    public CursoController(CursoService cursoService) {
        // Guarda el servicio
        this.cursoService = cursoService;
    }

    // Responde a GET /api/cursos
    @GetMapping
    // Devuelve la lista de cursos
    public List<CursoResponse> listar() {
        // Delega la lógica en el servicio
        return cursoService.listar();
    }
}
