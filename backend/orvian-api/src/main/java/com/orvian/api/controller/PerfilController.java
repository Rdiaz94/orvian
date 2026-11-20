// Paquete de los controladores de la capa de presentación
package com.orvian.api.controller;

// Respuesta HTTP con estado y cuerpo
import org.springframework.http.ResponseEntity;
// Inyecta el usuario autenticado en el método
import org.springframework.security.core.annotation.AuthenticationPrincipal;
// Token JWT ya validado
import org.springframework.security.oauth2.jwt.Jwt;
// Anotación para responder a peticiones PUT
import org.springframework.web.bind.annotation.PutMapping;
// Anotación que toma el cuerpo JSON de la petición
import org.springframework.web.bind.annotation.RequestBody;
// Anotación que agrupa las rutas bajo un prefijo común
import org.springframework.web.bind.annotation.RequestMapping;
// Anotación que indica que la clase responde con JSON
import org.springframework.web.bind.annotation.RestController;

// Datos para cambiar la contraseña
import com.orvian.api.dto.CambioContrasenaRequest;
// Datos que se pueden editar del perfil
import com.orvian.api.dto.PerfilRequest;
// Datos públicos del usuario
import com.orvian.api.dto.UsuarioResponse;
// Obtiene quién hace la petición
import com.orvian.api.security.Identidad;
// Servicio del perfil
import com.orvian.api.service.PerfilService;
// Petición HTTP del servlet
import jakarta.servlet.http.HttpServletRequest;
// Anotación que activa la validación del cuerpo recibido
import jakarta.validation.Valid;

// Declara el controlador REST del perfil
@RestController
// Todas las rutas de esta clase empiezan con /api/perfil
@RequestMapping("/api/perfil")
// Clase que permite al usuario editar su propia cuenta
public class PerfilController {

    // Servicio con la lógica de negocio
    private final PerfilService perfilService;

    // Spring entrega el servicio por el constructor
    public PerfilController(PerfilService perfilService) {
        // Guarda el servicio
        this.perfilService = perfilService;
    }

    // Responde a PUT /api/perfil
    @PutMapping
    // Valida los datos recibidos y actualiza el perfil del usuario del token
    public UsuarioResponse actualizar(@AuthenticationPrincipal Jwt token, @Valid @RequestBody PerfilRequest datos) {
        // El usuario sale del token, nunca de la petición
        return perfilService.actualizar(Identidad.idDe(token), datos);
    }

    // Responde a PUT /api/perfil/contrasena
    @PutMapping("/contrasena")
    // Valida los datos recibidos y cambia la contraseña; responde sin cuerpo
    public ResponseEntity<Void> cambiarContrasena(@AuthenticationPrincipal Jwt token, @Valid @RequestBody CambioContrasenaRequest datos, HttpServletRequest peticion) {
        // El usuario sale del token y la dirección IP alimenta el límite de intentos
        perfilService.cambiarContrasena(Identidad.idDe(token), peticion.getRemoteAddr(), datos);
        // Responde 204 No Content
        return ResponseEntity.noContent().build();
    }
}
