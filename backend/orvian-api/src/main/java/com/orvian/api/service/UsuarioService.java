// Paquete de la capa de lógica de negocio
package com.orvian.api.service;

// Resultado paginado de una consulta
import org.springframework.data.domain.Page;
// Define el orden de una consulta
import org.springframework.data.domain.Sort;
// Anotación que marca la clase como servicio
import org.springframework.stereotype.Service;
// Anotación que ejecuta el método dentro de una transacción
import org.springframework.transaction.annotation.Transactional;

// Datos públicos del usuario
import com.orvian.api.dto.UsuarioResponse;
// Repositorio de usuarios
import com.orvian.api.repository.UsuarioRepository;
// Reglas comunes de paginación
import com.orvian.api.util.Paginacion;

// Lógica de negocio de la gestión de usuarios
@Service
// Clase que consulta usuarios para el administrador
public class UsuarioService {

    // Acceso a los datos de usuarios
    private final UsuarioRepository usuarios;

    // Spring entrega el repositorio por el constructor
    public UsuarioService(UsuarioRepository usuarios) {
        // Guarda el repositorio
        this.usuarios = usuarios;
    }

    // Solo lee datos, así que la transacción es de solo lectura
    @Transactional(readOnly = true)
    // Devuelve una página de usuarios con sus datos públicos
    public Page<UsuarioResponse> listar(int pagina, int tamano) {
        // El orden es fijo: así el cliente no puede ordenar por campos internos como el hash
        return usuarios.findAll(Paginacion.de(pagina, tamano, Sort.by("id"))).map(UsuarioResponse::desde);
    }
}
