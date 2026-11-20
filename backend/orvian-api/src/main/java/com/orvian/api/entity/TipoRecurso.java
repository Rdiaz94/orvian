// Paquete de las entidades de la capa de datos
package com.orvian.api.entity;

// Tipos de recurso que se pueden compartir en la biblioteca
public enum TipoRecurso {
    // Archivo PDF alojado en un enlace
    PDF,
    // Video alojado en un enlace
    VIDEO,
    // Documento de texto, hoja de cálculo o presentación
    DOCUMENTO,
    // Página web o repositorio
    ENLACE,
    // Cuestionario de práctica
    QUIZ
}
