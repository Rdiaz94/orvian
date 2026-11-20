// Paquete de los controladores de la capa de presentación
package com.orvian.api.controller;

// Inyecta el usuario autenticado en el método
import org.springframework.security.core.annotation.AuthenticationPrincipal;
// Token JWT ya validado
import org.springframework.security.oauth2.jwt.Jwt;
// Anotación para responder a peticiones GET
import org.springframework.web.bind.annotation.GetMapping;
// Anotación que agrupa las rutas bajo un prefijo común
import org.springframework.web.bind.annotation.RequestMapping;
// Anotación que indica que la clase responde con JSON
import org.springframework.web.bind.annotation.RestController;

// Resumen de la actividad académica
import com.orvian.api.dto.ResumenAcademico;
// Obtiene quién hace la petición
import com.orvian.api.security.Identidad;
// Servicio de resumen
import com.orvian.api.service.ResumenService;

// Declara el controlador REST del resumen
@RestController
// Todas las rutas de esta clase empiezan con /api/resumen
@RequestMapping("/api/resumen")
// Clase que expone el resumen académico del usuario autenticado
public class ResumenController {

    // Servicio con la lógica de negocio
    private final ResumenService resumenService;

    // Spring entrega el servicio por el constructor
    public ResumenController(ResumenService resumenService) {
        // Guarda el servicio
        this.resumenService = resumenService;
    }

    // Responde a GET /api/resumen
    @GetMapping
    // Devuelve el resumen del usuario autenticado
    public ResumenAcademico obtener(@AuthenticationPrincipal Jwt token) {
        // El usuario sale del token
        return resumenService.obtener(Identidad.idDe(token));
    }
}
