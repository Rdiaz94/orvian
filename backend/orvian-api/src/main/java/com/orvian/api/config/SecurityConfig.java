// Paquete de las clases de configuración
package com.orvian.api.config;

// Tipos de despacho de una petición dentro del servidor
import jakarta.servlet.DispatcherType;
// Registra componentes que Spring administra
import org.springframework.context.annotation.Bean;
// Anotación que marca la clase como fuente de configuración
import org.springframework.context.annotation.Configuration;
// Permite usar anotaciones de permisos en los métodos, como @PreAuthorize
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
// Utilidad para usar la configuración por defecto de una función
import org.springframework.security.config.Customizer;
// Objeto con el que se configura la seguridad web
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// Utilidad para desactivar una función de seguridad
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
// Política de creación de sesiones
import org.springframework.security.config.http.SessionCreationPolicy;
// Algoritmo BCrypt para cifrar contraseñas
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// Interfaz común de los cifradores de contraseñas
import org.springframework.security.crypto.password.PasswordEncoder;
// Convierte el token en el usuario autenticado
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
// Convierte un dato del token en permisos
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
// Cadena de filtros que procesa cada petición
import org.springframework.security.web.SecurityFilterChain;

// Indica que esta clase define componentes de configuración
@Configuration
// Activa la seguridad por anotaciones en los métodos
@EnableMethodSecurity
// Configuración de seguridad del backend de Orvian
public class SecurityConfig {

    // Registra la cadena de filtros que Spring Security aplica a cada petición
    @Bean
    // Recibe la configuración de seguridad y el manejador de errores
    public SecurityFilterChain filterChain(HttpSecurity http, ManejadorErroresSeguridad manejadorErrores) throws Exception {
        // Empieza a configurar la seguridad
        http
                // Aplica las reglas CORS definidas en CorsConfig, antes de evaluar los permisos
                .cors(Customizer.withDefaults())
                // Desactiva CSRF: la API es sin estado y usa tokens en cabeceras, no cookies
                .csrf(AbstractHttpConfigurer::disable)
                // No crea sesiones en el servidor: cada petición lleva su propio token
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Desactiva el formulario de login que trae Spring por defecto
                .formLogin(AbstractHttpConfigurer::disable)
                // Desactiva el login básico del navegador
                .httpBasic(AbstractHttpConfigurer::disable)
                // Define qué rutas son públicas y cuáles exigen autenticación
                .authorizeHttpRequests(rutas -> rutas
                        // Permite que el servidor entregue sus páginas de error; la petición original sí se autorizó antes
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // Solo estas rutas son públicas: estado del servicio, registro e inicio de sesión
                        .requestMatchers("/api/health", "/api/auth/registro", "/api/auth/login").permitAll()
                        // La documentación es pública, pero solo existe si se enciende en la configuración local
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Las rutas de administración exigen el rol ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Cualquier otra ruta exige un token válido
                        .anyRequest().authenticated())
                // Valida el token JWT enviado en la cabecera Authorization: Bearer
                .oauth2ResourceServer(recurso -> recurso
                        // Usa el convertidor que traduce el rol del token en permisos
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorJwt()))
                        // Responde 401 en formato uniforme cuando falta el token o es inválido
                        .authenticationEntryPoint(manejadorErrores)
                        // Responde 403 en formato uniforme cuando faltan permisos
                        .accessDeniedHandler(manejadorErrores));
        // Construye y devuelve la cadena de filtros
        return http.build();
    }

    // Registra el cifrador de contraseñas para usarlo en toda la aplicación
    @Bean
    // Método que crea el cifrador
    public PasswordEncoder passwordEncoder() {
        // BCrypt agrega una sal aleatoria y es lento a propósito para resistir ataques
        return new BCryptPasswordEncoder();
    }

    // Traduce el rol del token en un permiso de Spring Security
    private JwtAuthenticationConverter convertidorJwt() {
        // Convierte un dato del token en permisos
        JwtGrantedAuthoritiesConverter permisos = new JwtGrantedAuthoritiesConverter();
        // El rol viaja en el dato "rol" del token
        permisos.setAuthoritiesClaimName("rol");
        // Spring espera el prefijo ROLE_ para usar hasRole('ADMIN')
        permisos.setAuthorityPrefix("ROLE_");
        // Crea el convertidor del token completo
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        // Usa los permisos calculados a partir del rol
        convertidor.setJwtGrantedAuthoritiesConverter(permisos);
        // Devuelve el convertidor configurado
        return convertidor;
    }
}
