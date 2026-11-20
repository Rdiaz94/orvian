// Utilidades de Angular
import { Injectable, inject } from '@angular/core';
// Cliente HTTP y parámetros de URL
import { HttpClient, HttpParams } from '@angular/common/http';
// Tipo de flujo de datos
import { Observable } from 'rxjs';

// Dirección de la API según el entorno
import { environment } from '../../../environments/environment';
// Tipos de datos
import { AvanceRequest, EstadoTarea, Pagina, Tarea, TareaGrupoRequest } from '../modelos';

// Consulta y modifica las tareas compartidas de un grupo
@Injectable({ providedIn: 'root' })
export class TareaGrupoApi {
  // Cliente HTTP
  private readonly http = inject(HttpClient);

  // Dirección de las tareas de un grupo
  private base(grupoId: number): string {
    // Construye la ruta
    return `${environment.apiUrl}/api/grupos/${grupoId}/tareas`;
  }

  // Lista las tareas del grupo, con filtro opcional por estado
  listar(grupoId: number, estado?: EstadoTarea, pagina = 0, tamano = 20): Observable<Pagina<Tarea>> {
    // Parámetros de paginación
    let params = new HttpParams().set('pagina', pagina).set('tamano', tamano);
    // Si se pidió un estado, se agrega al filtro
    if (estado) {
      // Agrega el estado
      params = params.set('estado', estado);
    }
    // Consulta la API
    return this.http.get<Pagina<Tarea>>(this.base(grupoId), { params });
  }

  // Crea una tarea en el grupo; solo el líder puede asignarla a otra persona
  crear(grupoId: number, datos: TareaGrupoRequest): Observable<Tarea> {
    // Envía los datos a la API
    return this.http.post<Tarea>(this.base(grupoId), datos);
  }

  // Edita una tarea del grupo; solo el líder
  actualizar(grupoId: number, tareaId: number, datos: TareaGrupoRequest): Observable<Tarea> {
    // Envía los datos a la API
    return this.http.put<Tarea>(`${this.base(grupoId)}/${tareaId}`, datos);
  }

  // Cambia el estado y el avance; la persona asignada o el líder
  registrarAvance(grupoId: number, tareaId: number, datos: AvanceRequest): Observable<Tarea> {
    // Envía el cambio a la API
    return this.http.patch<Tarea>(`${this.base(grupoId)}/${tareaId}/avance`, datos);
  }

  // Borra una tarea del grupo; solo el líder
  eliminar(grupoId: number, tareaId: number): Observable<void> {
    // Envía la orden a la API
    return this.http.delete<void>(`${this.base(grupoId)}/${tareaId}`);
  }
}
