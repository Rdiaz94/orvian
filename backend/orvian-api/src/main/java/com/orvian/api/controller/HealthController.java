// Paquete de los controladores de la capa de presentación
package com.orvian.api.controller;

// Estructura inmutable para devolver pares clave y valor
import java.util.Map;
// Anotación para responder a peticiones GET
import org.springframework.web.bind.annotation.GetMapping;
// Anotación que agrupa las rutas bajo un prefijo común
import org.springframework.web.bind.annotation.RequestMapping;
// Anotación que indica que la clase responde con JSON
import org.springframework.web.bind.annotation.RestController;

// Declara el controlador REST de comprobación del servicio
@RestController
// Todas las rutas de esta clase empiezan con /api
@RequestMapping("/api")
// Clase que verifica que el backend está activo
public class HealthController {

    // Responde a GET /api/health
    @GetMapping("/health")
    // Devuelve el estado del servicio en formato JSON
    public Map<String, String> health() {
        // Respuesta simple que confirma que la API funciona
        return Map.of("estado", "ok", "aplicacion", "Orvian API");
    }
}
