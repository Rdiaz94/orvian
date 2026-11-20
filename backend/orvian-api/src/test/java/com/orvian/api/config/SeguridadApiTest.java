// Paquete de las pruebas de la configuración
package com.orvian.api.config;

// Verificaciones legibles de Mockito
import static org.mockito.ArgumentMatchers.any;
// Comodín para un valor exacto en Mockito
import static org.mockito.ArgumentMatchers.eq;
// Comodín para un valor nulo en Mockito
import static org.mockito.ArgumentMatchers.isNull;
// Hace que un método simulado lance una excepción
import static org.mockito.Mockito.doThrow;
// Verifica que un método se llamó
import static org.mockito.Mockito.verify;
// Define el comportamiento de un objeto simulado
import static org.mockito.Mockito.when;
// Tokens simulados para las pruebas de seguridad
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
// Construcción de peticiones HTTP de prueba
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
// Construcción de peticiones GET de prueba
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// Construcción de peticiones OPTIONS de prueba
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
// Construcción de peticiones POST de prueba
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// Construcción de peticiones PUT de prueba
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
// Verificaciones del contenido de la respuesta
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
// Verificaciones de las cabeceras de la respuesta
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
// Verificaciones del JSON de la respuesta
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// Verificaciones del estado de la respuesta
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Anotación que marca un método como prueba
import org.junit.jupiter.api.Test;
// Anotación para pruebas con varios valores de entrada
import org.junit.jupiter.params.ParameterizedTest;
// Origen de los valores de una prueba parametrizada
import org.junit.jupiter.params.provider.ValueSource;
// Inyecta un componente de Spring en la prueba
import org.springframework.beans.factory.annotation.Autowired;
// Prueba que carga solo la capa web
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// Importa clases de configuración adicionales
import org.springframework.context.annotation.Import;
// Tipos de contenido HTTP
import org.springframework.http.MediaType;
// Permiso de Spring Security
import org.springframework.security.core.authority.SimpleGrantedAuthority;
// Decodificador de tokens, que se simula
import org.springframework.security.oauth2.jwt.BadJwtException;
// Decodificador de tokens
import org.springframework.security.oauth2.jwt.JwtDecoder;
// Reemplaza un componente de Spring por un simulado
import org.springframework.test.context.bean.override.mockito.MockitoBean;
// Cliente simulado para probar la capa web
import org.springframework.test.web.servlet.MockMvc;
// Modifica una petición antes de enviarla
import org.springframework.test.web.servlet.request.RequestPostProcessor;
// Resultado paginado de una consulta
import org.springframework.data.domain.Page;

// Controlador de administración
import com.orvian.api.controller.AdminController;
// Controlador de autenticación
import com.orvian.api.controller.AuthController;
// Controlador de cursos
import com.orvian.api.controller.CursoController;
// Controlador de grupos
import com.orvian.api.controller.GrupoController;
// Controlador de estado del servicio
import com.orvian.api.controller.HealthController;
// Controlador del perfil
import com.orvian.api.controller.PerfilController;
// Controlador de recursos
import com.orvian.api.controller.RecursoController;
// Controlador del resumen
import com.orvian.api.controller.ResumenController;
// Controlador de tareas de grupo
import com.orvian.api.controller.TareaGrupoController;
// Controlador de tareas personales
import com.orvian.api.controller.TareaController;
// Datos públicos del usuario
import com.orvian.api.dto.UsuarioResponse;
// Excepción de conflicto con datos existentes
import com.orvian.api.exception.ConflictoException;
// Excepción por exceso de intentos
import com.orvian.api.exception.DemasiadosIntentosException;
// Excepción cuando un recurso no existe o es ajeno
import com.orvian.api.exception.NoEncontradoException;
// Excepción cuando falta el permiso para la acción
import com.orvian.api.exception.PermisoInsuficienteException;
// Servicio de autenticación
import com.orvian.api.service.AuthService;
// Servicio de cursos
import com.orvian.api.service.CursoService;
// Servicio de grupos
import com.orvian.api.service.GrupoService;
// Servicio del perfil
import com.orvian.api.service.PerfilService;
// Servicio de recursos
import com.orvian.api.service.RecursoService;
// Servicio del resumen
import com.orvian.api.service.ResumenService;
// Servicio de tareas
import com.orvian.api.service.TareaService;
// Servicio de usuarios
import com.orvian.api.service.UsuarioService;

// Carga solo la capa web con la configuración de seguridad real y los servicios simulados
@WebMvcTest({HealthController.class, AuthController.class, CursoController.class, AdminController.class, TareaController.class,
        TareaGrupoController.class, GrupoController.class, RecursoController.class, ResumenController.class, PerfilController.class})
// Incorpora la seguridad, los errores de seguridad y CORS reales de la aplicación
@Import({SecurityConfig.class, ManejadorErroresSeguridad.class, CorsConfig.class})
// Red de seguridad: comprueba sin base de datos quién puede entrar a qué y cómo responde la API a los errores
class SeguridadApiTest {

    // Cliente simulado para llamar a la API
    @Autowired
    private MockMvc mvc;

    // La seguridad exige un decodificador de tokens; aquí se simula
    @MockitoBean
    private JwtDecoder decodificador;
    // Servicio de autenticación simulado
    @MockitoBean
    private AuthService authService;
    // Servicio de cursos simulado
    @MockitoBean
    private CursoService cursoService;
    // Servicio de usuarios simulado
    @MockitoBean
    private UsuarioService usuarioService;
    // Servicio de tareas simulado
    @MockitoBean
    private TareaService tareaService;
    // Servicio de grupos simulado
    @MockitoBean
    private GrupoService grupoService;
    // Servicio de recursos simulado
    @MockitoBean
    private RecursoService recursoService;
    // Servicio del resumen simulado
    @MockitoBean
    private ResumenService resumenService;
    // Servicio del perfil simulado
    @MockitoBean
    private PerfilService perfilService;

    // Token de un estudiante con identificador 1
    private static RequestPostProcessor estudiante() {
        // Simula un token cuyo sujeto es el usuario 1 y que trae el permiso de estudiante
        return jwt().jwt(token -> token.subject("1")).authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"));
    }

    // Token de un administrador con identificador 1
    private static RequestPostProcessor administrador() {
        // Simula un token cuyo sujeto es el usuario 1 y que trae el permiso de administrador
        return jwt().jwt(token -> token.subject("1")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    // Marca el método como prueba
    @Test
    // La comprobación de estado no pide token
    void laRutaDeEstadoEsPublica() throws Exception {
        // Sin token debe responder 200
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    // Prueba con una ruta distinta en cada ejecución
    @ParameterizedTest
    // Rutas protegidas de cada módulo
    @ValueSource(strings = {"/api/tareas", "/api/grupos", "/api/recursos", "/api/cursos", "/api/auth/yo", "/api/admin/usuarios", "/api/resumen"})
    // Sin token todas deben responder 401 en el formato uniforme
    void lasRutasProtegidasSinTokenDan401(String ruta) throws Exception {
        // Llama sin token
        mvc.perform(get(ruta))
                // El estado es 401
                .andExpect(status().isUnauthorized())
                // El formato es el estándar de errores
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                // El título es el esperado
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    // Marca el método como prueba
    @Test
    // Un token inválido da 401 con un mensaje genérico que no revela detalles de la librería
    void unTokenInvalidoDa401SinDetallesInternos() throws Exception {
        // El decodificador rechaza el token con un mensaje interno
        when(decodificador.decode("token-falso")).thenThrow(new BadJwtException("detalle interno de la librería"));

        // Llama con un token inventado
        mvc.perform(get("/api/tareas").header("Authorization", "Bearer token-falso"))
                // El estado es 401
                .andExpect(status().isUnauthorized())
                // El detalle es genérico y no contiene el mensaje interno
                .andExpect(jsonPath("$.detail").value("Debes iniciar sesión con un token válido"));
    }

    // Marca el método como prueba
    @Test
    // Un estudiante no puede entrar a la administración
    void unEstudianteNoEntraALaAdministracion() throws Exception {
        // Llama con un token de estudiante
        mvc.perform(get("/api/admin/usuarios").with(estudiante()))
                // El estado es 403
                .andExpect(status().isForbidden())
                // El título es el esperado
                .andExpect(jsonPath("$.title").value("Acceso denegado"));
    }

    // Marca el método como prueba
    @Test
    // Un estudiante no puede crear cursos
    void unEstudianteNoCreaCursos() throws Exception {
        // Llama con un token de estudiante
        mvc.perform(post("/api/admin/cursos").with(estudiante()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"X\"}"))
                // El estado es 403
                .andExpect(status().isForbidden());
    }

    // Marca el método como prueba
    @Test
    // Un administrador sí puede listar usuarios
    void unAdministradorListaUsuarios() throws Exception {
        // El servicio devuelve una página vacía
        when(usuarioService.listar(0, 20)).thenReturn(Page.<UsuarioResponse>empty());

        // Llama con un token de administrador
        mvc.perform(get("/api/admin/usuarios").with(administrador()))
                // El estado es 200
                .andExpect(status().isOk());
    }

    // Marca el método como prueba
    @Test
    // El inicio de sesión es público y valida los datos
    void elLoginEsPublicoYValidaLosDatos() throws Exception {
        // Llama sin token con datos vacíos
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"\",\"contrasena\":\"\"}"))
                // El estado es 400, no 401: la ruta es pública
                .andExpect(status().isBadRequest())
                // Indica el problema de cada campo
                .andExpect(jsonPath("$.errores.correo").exists())
                // Indica el problema de la contraseña
                .andExpect(jsonPath("$.errores.contrasena").exists());
    }

    // Marca el método como prueba
    @Test
    // El registro es público y valida los datos
    void elRegistroEsPublicoYValidaLosDatos() throws Exception {
        // Llama sin token con un cuerpo vacío
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                // El estado es 400, no 401: la ruta es pública
                .andExpect(status().isBadRequest());
    }

    // Marca el método como prueba
    @Test
    // Los datos inválidos responden en el formato uniforme con el detalle por campo
    void losDatosInvalidosDanUn400ConDetallePorCampo() throws Exception {
        // Llama con un título vacío
        mvc.perform(post("/api/tareas").with(estudiante()).contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"\"}"))
                // El estado es 400
                .andExpect(status().isBadRequest())
                // El título del error es el esperado
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                // Indica el problema del título
                .andExpect(jsonPath("$.errores.titulo").exists());
    }

    // Marca el método como prueba
    @Test
    // Un JSON mal formado responde con un mensaje genérico
    void unJsonMalFormadoDa400() throws Exception {
        // Llama con un JSON incompleto
        mvc.perform(post("/api/tareas").with(estudiante()).contentType(MediaType.APPLICATION_JSON).content("{"))
                // El estado es 400
                .andExpect(status().isBadRequest())
                // El título del error es el esperado
                .andExpect(jsonPath("$.title").value("Cuerpo ilegible"));
    }

    // Marca el método como prueba
    @Test
    // Un valor inválido en un parámetro de la URL responde 400
    void unEstadoInvalidoEnElFiltroDa400() throws Exception {
        // Llama con un estado inventado
        mvc.perform(get("/api/tareas").param("estado", "INVENTADO").with(estudiante()))
                // El estado es 400
                .andExpect(status().isBadRequest());
    }

    // Marca el método como prueba
    @Test
    // El usuario que usan los servicios sale del token, no de la petición
    void elUsuarioSalDelToken() throws Exception {
        // El servicio devuelve una página vacía
        when(tareaService.listar(eq(1L), isNull(), eq(0), eq(20))).thenReturn(Page.empty());

        // Llama con un token cuyo sujeto es el usuario 1
        mvc.perform(get("/api/tareas").with(estudiante())).andExpect(status().isOk());

        // El servicio recibió el identificador del token
        verify(tareaService).listar(eq(1L), isNull(), eq(0), eq(20));
    }

    // Marca el método como prueba
    @Test
    // Un recurso inexistente o ajeno responde 404
    void unRecursoAjenoDa404() throws Exception {
        // El servicio indica que no existe
        when(tareaService.obtener(1L, 5L)).thenThrow(new NoEncontradoException("Tarea no encontrada"));

        // Llama con un token de estudiante
        mvc.perform(get("/api/tareas/5").with(estudiante()))
                // El estado es 404
                .andExpect(status().isNotFound())
                // El título es el esperado
                .andExpect(jsonPath("$.title").value("No encontrado"));
    }

    // Marca el método como prueba
    @Test
    // Una acción sin permiso dentro de un grupo responde 403
    void unPermisoInsuficienteDa403() throws Exception {
        // El servicio indica que falta permiso
        doThrow(new PermisoInsuficienteException("Solo el líder del grupo puede hacer esto")).when(grupoService).eliminar(1L, 7L);

        // Llama con un token de estudiante
        mvc.perform(delete("/api/grupos/7").with(estudiante()))
                // El estado es 403
                .andExpect(status().isForbidden())
                // El título es el esperado
                .andExpect(jsonPath("$.title").value("Permiso insuficiente"));
    }

    // Marca el método como prueba
    @Test
    // Un conflicto con datos existentes responde 409
    void unConflictoDa409() throws Exception {
        // El servicio indica que ya existe
        when(grupoService.agregarIntegrante(eq(1L), eq(7L), any())).thenThrow(new ConflictoException("Esa persona ya es integrante del grupo"));

        // Llama con un token de estudiante
        mvc.perform(post("/api/grupos/7/integrantes").with(estudiante()).contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"ana@correo.com\"}"))
                // El estado es 409
                .andExpect(status().isConflict());
    }

    // Marca el método como prueba
    @Test
    // Un método HTTP no admitido responde 405
    void unMetodoNoAdmitidoDa405() throws Exception {
        // Llama con PUT a una ruta que solo admite GET
        mvc.perform(put("/api/cursos").with(estudiante()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                // El estado es 405
                .andExpect(status().isMethodNotAllowed());
    }

    // Marca el método como prueba
    @Test
    // Un enlace javascript: en un recurso se rechaza
    void unEnlaceJavascriptSeRechaza() throws Exception {
        // Llama con un enlace peligroso
        mvc.perform(post("/api/recursos").with(estudiante()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"X\",\"tipo\":\"ENLACE\",\"url\":\"javascript:alert(1)\"}"))
                // El estado es 400
                .andExpect(status().isBadRequest())
                // Indica el problema del enlace
                .andExpect(jsonPath("$.errores.url").exists());
    }

    // Marca el método como prueba
    @Test
    // CORS permite al frontend autorizado
    void corsPermiteAlFrontendAutorizado() throws Exception {
        // Simula la verificación previa del navegador desde el frontend
        mvc.perform(options("/api/tareas").header("Origin", "http://localhost:4200").header("Access-Control-Request-Method", "GET"))
                // El estado es 200
                .andExpect(status().isOk())
                // Autoriza ese origen
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    // Marca el método como prueba
    @Test
    // CORS bloquea cualquier otro sitio
    void corsBloqueaOtrosOrigenes() throws Exception {
        // Simula la verificación previa desde un sitio desconocido
        mvc.perform(options("/api/tareas").header("Origin", "http://sitio-malo.com").header("Access-Control-Request-Method", "GET"))
                // El estado es 403
                .andExpect(status().isForbidden());
    }

    // Marca el método como prueba
    @Test
    // El exceso de intentos responde 429 con la espera indicada y sin detalles de la cuenta
    void elExcesoDeIntentosDa429ConRetryAfter() throws Exception {
        // El servicio indica que hay que esperar 120 segundos
        when(authService.login(any(), any())).thenThrow(new DemasiadosIntentosException(120));

        // Llama sin token con datos válidos, porque el login es público
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"ana@correo.com\",\"contrasena\":\"Clave-123456\"}"))
                // El estado es 429
                .andExpect(status().isTooManyRequests())
                // Indica cuántos segundos esperar
                .andExpect(header().string("Retry-After", "120"))
                // El título es el esperado
                .andExpect(jsonPath("$.title").value("Demasiados intentos"));
    }

    // Marca el método como prueba
    @Test
    // Editar el perfil exige sesión
    void elPerfilExigeSesion() throws Exception {
        // Llama sin token
        mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ana\"}"))
                // El estado es 401
                .andExpect(status().isUnauthorized());
    }

    // Marca el método como prueba
    @Test
    // El perfil valida el nombre
    void elPerfilValidaElNombre() throws Exception {
        // Llama con un nombre vacío
        mvc.perform(put("/api/perfil").with(estudiante()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                // El estado es 400
                .andExpect(status().isBadRequest())
                // Indica el problema del nombre
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    // Marca el método como prueba
    @Test
    // La contraseña nueva debe tener una longitud válida
    void laContrasenaNuevaDebeTenerLongitudValida() throws Exception {
        // Llama con una contraseña nueva demasiado corta
        mvc.perform(put("/api/perfil/contrasena").with(estudiante()).contentType(MediaType.APPLICATION_JSON).content("{\"contrasenaActual\":\"x\",\"contrasenaNueva\":\"123\"}"))
                // El estado es 400
                .andExpect(status().isBadRequest())
                // Indica el problema de la contraseña nueva
                .andExpect(jsonPath("$.errores.contrasenaNueva").exists());
    }

    // Marca el método como prueba
    @Test
    // Un cambio de contraseña correcto responde sin contenido
    void unCambioDeContrasenaCorrectoDa204() throws Exception {
        // Llama con datos válidos
        mvc.perform(put("/api/perfil/contrasena").with(estudiante()).contentType(MediaType.APPLICATION_JSON).content("{\"contrasenaActual\":\"Actual-123\",\"contrasenaNueva\":\"Nueva-12345\"}"))
                // El estado es 204
                .andExpect(status().isNoContent());
    }
}
