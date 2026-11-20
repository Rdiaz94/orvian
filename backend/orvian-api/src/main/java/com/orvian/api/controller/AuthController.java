// Paquete de los controladores de la capa de presentación
package com.orvian.api.controller;

// Constantes de los códigos de estado HTTP
import org.springframework.http.HttpStatus;
// Respuesta HTTP con estado y cuerpo
import org.springframework.http.ResponseEntity;
// Inyecta el usuario autenticado en el método
import org.springframework.security.core.annotation.AuthenticationPrincipal;
// Token JWT ya validado
import org.springframework.security.oauth2.jwt.Jwt;
// Anotación para responder a peticiones GET
import org.springframework.web.bind.annotation.GetMapping;
// Anotación para recibir datos en una petición POST
import org.springframework.web.bind.annotation.PostMapping;
// Anotación que toma el cuerpo JSON de la petición
import org.springframework.web.bind.annotation.RequestBody;
// Anotación que agrupa las rutas bajo un prefijo común
import org.springframework.web.bind.annotation.RequestMapping;
// Anotación que indica que la clase responde con JSON
import org.springframework.web.bind.annotation.RestController;

// Respuesta del inicio de sesión
import com.orvian.api.dto.AuthResponse;
// Datos de entrada del inicio de sesión
import com.orvian.api.dto.LoginRequest;
// Datos de entrada del registro
import com.orvian.api.dto.RegistroRequest;
// Datos públicos del usuario
import com.orvian.api.dto.UsuarioResponse;
// Obtiene quién hace la petición
import com.orvian.api.security.Identidad;
// Servicio de autenticación
import com.orvian.api.service.AuthService;
// Petición HTTP del servlet
import jakarta.servlet.http.HttpServletRequest;
// Anotación que activa la validación del cuerpo recibido
import jakarta.validation.Valid;

// Declara el controlador REST de autenticación
@RestController
// Todas las rutas de esta clase empiezan con /api/auth
@RequestMapping("/api/auth")
// Clase que recibe las peticiones de registro, inicio de sesión y perfil
public class AuthController {

    // Servicio con la lógica de negocio
    private final AuthService authService;

    // Spring entrega el servicio por el constructor
    public AuthController(AuthService authService) {
        // Guarda el servicio
        this.authService = authService;
    }

    // Responde a POST /api/auth/registro
    @PostMapping("/registro")
    // Valida los datos recibidos antes de ejecutar el método
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest datos) {
        // Delega la lógica en el servicio
        UsuarioResponse creado = authService.registrar(datos);
        // Responde 201 Created con los datos públicos del usuario
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // Responde a POST /api/auth/login
    @PostMapping("/login")
    // Valida los datos recibidos y devuelve el token con 200 OK
    public AuthResponse login(@Valid @RequestBody LoginRequest datos, HttpServletRequest peticion) {
        // Delega la lógica en el servicio con la dirección IP desde la que llega la petición
        return authService.login(datos, peticion.getRemoteAddr());
    }

    // Responde a GET /api/auth/yo y exige un token válido
    @GetMapping("/yo")
    // Spring entrega el token ya validado por el filtro de seguridad
    public UsuarioResponse perfil(@AuthenticationPrincipal Jwt token) {
        // El sujeto del token es el identificador del usuario
        return authService.perfil(Identidad.idDe(token));
    }
}
