// Roles que puede tener un usuario
export type Rol = 'ESTUDIANTE' | 'ADMIN';
// Estados por los que pasa una tarea
export type EstadoTarea = 'PENDIENTE' | 'EN_PROGRESO' | 'HECHA';
// Tipos de tarea que se muestran como etiquetas
export type EtiquetaTarea = 'PROYECTO' | 'LABORATORIO' | 'PRACTICA' | 'LECTURA' | 'QUIZ' | 'RESUMEN' | 'OTRO';
// Tipos de recurso de la biblioteca
export type TipoRecurso = 'PDF' | 'VIDEO' | 'DOCUMENTO' | 'ENLACE' | 'QUIZ';
// Papeles dentro de un grupo
export type RolGrupo = 'LIDER' | 'MIEMBRO';
// Órdenes permitidos para la biblioteca
export type OrdenRecurso = 'POPULARES' | 'RECIENTES';

// Datos públicos de un usuario, tal como los devuelve la API
export interface Usuario {
  // Identificador del usuario
  id: number;
  // Nombre completo
  nombre: string;
  // Correo electrónico
  correo: string;
  // Rol dentro de la plataforma
  rol: Rol;
  // Carrera que estudia
  carrera: string | null;
  // Ciclo académico
  ciclo: number | null;
}

// Datos que se envían para iniciar sesión
export interface LoginRequest {
  // Correo electrónico
  correo: string;
  // Contraseña en texto plano; solo viaja hasta el servidor
  contrasena: string;
}

// Datos que se envían para registrarse
export interface RegistroRequest {
  // Nombre completo
  nombre: string;
  // Correo electrónico
  correo: string;
  // Contraseña elegida
  contrasena: string;
  // Carrera (opcional)
  carrera: string | null;
  // Ciclo (opcional)
  ciclo: number | null;
}

// Respuesta del inicio de sesión
export interface AuthResponse {
  // Token JWT que se envía en cada petición
  token: string;
  // Tipo de token; siempre Bearer
  tipo: string;
  // Segundos que faltan para que expire
  expiraEnSegundos: number;
  // Datos públicos del usuario
  usuario: Usuario;
}

// Tarea tal como la devuelve la API
export interface Tarea {
  // Identificador de la tarea
  id: number;
  // Título de la tarea
  titulo: string;
  // Descripción de la tarea
  descripcion: string | null;
  // Identificador del curso, si tiene
  cursoId: number | null;
  // Nombre del curso, si tiene
  cursoNombre: string | null;
  // Identificador del grupo, si es una tarea de grupo
  grupoId: number | null;
  // Nombre del grupo, si es una tarea de grupo
  grupoNombre: string | null;
  // Identificador de la persona responsable
  responsableId: number;
  // Nombre de la persona responsable
  responsableNombre: string;
  // Estado actual
  estado: EstadoTarea;
  // Etiqueta de la tarea
  etiqueta: EtiquetaTarea;
  // Fecha límite de entrega en formato ISO 8601
  fechaVencimiento: string | null;
  // Porcentaje de avance
  progreso: number;
  // Momento de creación
  fechaCreacion: string;
}

// Grupo de trabajo desde el punto de vista de un integrante
export interface Grupo {
  // Identificador del grupo
  id: number;
  // Nombre del grupo
  nombre: string;
  // Descripción del grupo
  descripcion: string | null;
  // Identificador del curso, si tiene
  cursoId: number | null;
  // Nombre del curso, si tiene
  cursoNombre: string | null;
  // Papel de quien consulta dentro del grupo
  miRol: RolGrupo;
}

// Recurso de la biblioteca
export interface Recurso {
  // Identificador del recurso
  id: number;
  // Título del recurso
  titulo: string;
  // Tipo de recurso
  tipo: TipoRecurso;
  // Enlace donde está alojado
  url: string;
  // Descripción del recurso
  descripcion: string | null;
  // Identificador del curso, si tiene
  cursoId: number | null;
  // Nombre del curso, si tiene
  cursoNombre: string | null;
  // Identificador del grupo, si es un recurso de grupo
  grupoId: number | null;
  // Nombre del grupo, si es un recurso de grupo
  grupoNombre: string | null;
  // Identificador del autor
  autorId: number;
  // Nombre del autor
  autorNombre: string;
  // Cantidad de descargas
  descargas: number;
  // Momento en que se compartió
  fechaCreacion: string;
  // Indica si quien consulta es el autor
  esAutor: boolean;
}

// Próxima tarea por vencer
export interface ProximaEntrega {
  // Título de la tarea
  titulo: string;
  // Fecha y hora límite
  fechaVencimiento: string;
}

// Resumen de la actividad académica del usuario
export interface ResumenAcademico {
  // Cantidad total de tareas
  tareasTotales: number;
  // Tareas pendientes
  tareasPendientes: number;
  // Tareas en progreso
  tareasEnProgreso: number;
  // Tareas hechas
  tareasHechas: number;
  // Porcentaje de tareas hechas
  porcentajeCumplimiento: number;
  // Grupos a los que pertenece
  gruposActivos: number;
  // Recursos que compartió
  recursosCompartidos: number;
  // Próxima entrega, o nulo si no hay ninguna
  proximaEntrega: ProximaEntrega | null;
}

// Página de resultados con el formato que devuelve la API
export interface Pagina<T> {
  // Elementos de la página
  content: T[];
  // Datos de la paginación
  page: {
    // Elementos por página
    size: number;
    // Número de la página, empezando en cero
    number: number;
    // Total de elementos
    totalElements: number;
    // Total de páginas
    totalPages: number;
  };
}

// Curso al que se puede asociar una tarea, un recurso o un grupo
export interface Curso {
  // Identificador del curso
  id: number;
  // Nombre del curso
  nombre: string;
}

// Datos que se envían para crear o editar una tarea personal
export interface TareaRequest {
  // Título de la tarea
  titulo: string;
  // Descripción opcional
  descripcion: string | null;
  // Curso opcional
  cursoId: number | null;
  // Etiqueta opcional; si falta la API usa OTRO
  etiqueta: EtiquetaTarea | null;
  // Fecha límite opcional en formato ISO 8601
  fechaVencimiento: string | null;
}

// Datos que se envían para mover una tarea de estado o cambiar su avance
export interface AvanceRequest {
  // Nuevo estado
  estado: EstadoTarea;
  // Porcentaje de avance; solo cuenta cuando la tarea está en progreso
  progreso: number | null;
}

// Datos que se envían para compartir o editar un recurso
export interface RecursoRequest {
  // Título del recurso
  titulo: string;
  // Tipo de recurso
  tipo: TipoRecurso;
  // Enlace donde está alojado; debe ser http o https
  url: string;
  // Descripción opcional
  descripcion: string | null;
  // Curso opcional
  cursoId: number | null;
  // Grupo con el que se comparte; si es nulo, el recurso es público
  grupoId: number | null;
}

// Respuesta al registrar una descarga
export interface DescargaResponse {
  // Enlace que se debe abrir
  url: string;
}

// Datos que se envían para crear un grupo
export interface GrupoRequest {
  // Nombre del grupo
  nombre: string;
  // Descripción opcional
  descripcion: string | null;
  // Curso opcional
  cursoId: number | null;
}

// Integrante de un grupo; no incluye su correo
export interface Integrante {
  // Identificador del usuario
  usuarioId: number;
  // Nombre del integrante
  nombre: string;
  // Carrera del integrante
  carrera: string | null;
  // Ciclo del integrante
  ciclo: number | null;
  // Papel dentro del grupo
  rolGrupo: RolGrupo;
  // Momento en que ingresó al grupo
  fechaIngreso: string;
}

// Grupo con todos sus integrantes
export interface GrupoDetalle {
  // Datos del grupo
  grupo: Grupo;
  // Integrantes del grupo
  integrantes: Integrante[];
}

// Datos que se envían para crear o editar una tarea de grupo
export interface TareaGrupoRequest extends TareaRequest {
  // Persona a la que se asigna; si es nulo queda para quien la crea
  responsableId: number | null;
}

// Datos que el usuario puede cambiar de su perfil; el correo y el rol no se editan
export interface PerfilRequest {
  // Nombre completo
  nombre: string;
  // Carrera opcional
  carrera: string | null;
  // Ciclo opcional
  ciclo: number | null;
}

// Datos para cambiar la contraseña
export interface CambioContrasenaRequest {
  // Contraseña actual
  contrasenaActual: string;
  // Contraseña nueva
  contrasenaNueva: string;
}
