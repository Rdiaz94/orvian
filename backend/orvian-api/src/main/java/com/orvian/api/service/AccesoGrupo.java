// Paquete de la capa de lógica de negocio
package com.orvian.api.service;

// Anotación que registra la clase como componente
import org.springframework.stereotype.Component;

// Entidad de membresía
import com.orvian.api.entity.MembresiaGrupo;
// Papel dentro del grupo
import com.orvian.api.entity.RolGrupo;
// Excepción cuando un recurso no existe o es ajeno
import com.orvian.api.exception.NoEncontradoException;
// Excepción cuando falta el permiso para la acción
import com.orvian.api.exception.PermisoInsuficienteException;
// Repositorio de membresías
import com.orvian.api.repository.MembresiaGrupoRepository;

// Reglas de acceso a los grupos en un solo lugar; las usan grupos y tareas de grupo
@Component
// Clase que comprueba si un usuario pertenece a un grupo y qué papel tiene
public class AccesoGrupo {

    // Acceso a los datos de membresías
    private final MembresiaGrupoRepository membresias;

    // Spring entrega el repositorio por el constructor
    public AccesoGrupo(MembresiaGrupoRepository membresias) {
        // Guarda el repositorio
        this.membresias = membresias;
    }

    // Un grupo ajeno responde igual que uno inexistente, para no revelar qué grupos existen
    public MembresiaGrupo exigirIntegrante(Long usuarioId, Long grupoId) {
        // Busca la membresía del usuario en ese grupo
        return membresias.findByGrupoIdAndUsuarioId(grupoId, usuarioId).orElseThrow(() -> new NoEncontradoException("Grupo no encontrado"));
    }

    // Exige ser integrante y además tener el papel de líder
    public MembresiaGrupo exigirLider(Long usuarioId, Long grupoId) {
        // Primero debe pertenecer al grupo; si no, el grupo "no existe" para esa persona
        MembresiaGrupo propia = exigirIntegrante(usuarioId, grupoId);
        // Si pertenece pero no es líder, se le indica que le falta permiso
        if (propia.getRolGrupo() != RolGrupo.LIDER) {
            // Rechaza la acción por falta de permiso
            throw new PermisoInsuficienteException("Solo el líder del grupo puede hacer esto");
        }
        // Devuelve la membresía del líder
        return propia;
    }
}
