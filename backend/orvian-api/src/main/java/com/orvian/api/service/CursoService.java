// Paquete de la capa de lógica de negocio
package com.orvian.api.service;

// Lista de resultados
import java.util.List;

// Excepción de Spring cuando se viola una restricción de la base de datos
import org.springframework.dao.DataIntegrityViolationException;
// Anotación que marca la clase como servicio
import org.springframework.stereotype.Service;
// Anotación que ejecuta el método dentro de una transacción
import org.springframework.transaction.annotation.Transactional;

// Datos de entrada para crear un curso
import com.orvian.api.dto.CursoRequest;
// Datos públicos del curso
import com.orvian.api.dto.CursoResponse;
// Entidad de curso
import com.orvian.api.entity.Curso;
// Excepción de conflicto con datos existentes
import com.orvian.api.exception.ConflictoException;
// Excepción cuando se incumple una regla de negocio
import com.orvian.api.exception.SolicitudInvalidaException;
// Repositorio de cursos
import com.orvian.api.repository.CursoRepository;

// Lógica de negocio de los cursos
@Service
// Clase que lista y crea cursos
public class CursoService {

    // Acceso a los datos de cursos
    private final CursoRepository cursos;

    // Spring entrega el repositorio por el constructor
    public CursoService(CursoRepository cursos) {
        // Guarda el repositorio
        this.cursos = cursos;
    }

    // Solo lee datos, así que la transacción es de solo lectura
    @Transactional(readOnly = true)
    // Devuelve todos los cursos ordenados por nombre; son pocos, así que no se pagina
    public List<CursoResponse> listar() {
        // Convierte cada entidad en su versión pública
        return cursos.findAllByOrderByNombreAsc().stream().map(CursoResponse::desde).toList();
    }

    // Solo lee datos, así que la transacción es de solo lectura
    @Transactional(readOnly = true)
    // Devuelve el curso si se indicó un identificador; sin identificador devuelve nulo y si no existe, rechaza la petición
    public Curso buscarSiSeIndica(Long cursoId) {
        // Sin identificador no hay curso asociado
        if (cursoId == null) {
            // Retorna nulo
            return null;
        }
        // Con identificador, el curso debe existir
        return cursos.findById(cursoId).orElseThrow(() -> new SolicitudInvalidaException("El curso indicado no existe"));
    }

    // Todo el método se ejecuta en una sola transacción
    @Transactional
    // Crea un curso nuevo y devuelve sus datos públicos
    public CursoResponse crear(CursoRequest datos) {
        // Quita los espacios sobrantes del nombre
        String nombre = datos.nombre().trim();
        // Si ya existe un curso con ese nombre, se rechaza
        if (cursos.existsByNombreIgnoreCase(nombre)) {
            // Lanza el conflicto
            throw cursoRepetido(nombre);
        }
        // Intenta guardar el curso
        try {
            // Guarda y devuelve solo los datos públicos
            return CursoResponse.desde(cursos.save(new Curso(nombre)));
        // Si dos creaciones simultáneas chocan, la restricción única de la base de datos lo detecta
        } catch (DataIntegrityViolationException error) {
            // Se trata igual que un curso repetido
            throw cursoRepetido(nombre);
        }
    }

    // Construye el conflicto en un solo lugar para no repetir el mensaje
    private ConflictoException cursoRepetido(String nombre) {
        // Mensaje claro para el administrador
        return new ConflictoException("Ya existe un curso llamado " + nombre);
    }
}
