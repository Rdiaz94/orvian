// Utilidades de Angular
import { Injectable, inject } from '@angular/core';
// Cliente HTTP y parámetros de URL
import { HttpClient, HttpParams } from '@angular/common/http';
// Tipo de flujo de datos
import { Observable } from 'rxjs';

// Dirección de la API según el entorno
import { environment } from '../../../environments/environment';
// Tipos de datos
import { AvanceRequest, EstadoTarea, Pagina, Tarea, TareaRequest } from '../modelos';

// Consulta y modifica las tareas del usuario
@Injectable({ providedIn: 'root' })
export class TareaApi {
  // Cliente HTTP
  private readonly http = inject(HttpClient);
  // Dirección base de las tareas
  private readonly base = `${environment.apiUrl}/api/tareas`;

  // Lista las tareas asignadas al usuario, con filtro opcional por estado
  listar(estado?: EstadoTarea, pagina = 0, tamano = 20): Observable<Pagina<Tarea>> {
    // Parámetros de paginación
    let params = new HttpParams().set('pagina', pagina).set('tamano', tamano);
    // Si se pidió un estado, se agrega al filtro
    if (estado) {
      // Agrega el estado
      params = params.set('estado', estado);
    }
    // Consulta la API
    return this.http.get<Pagina<Tarea>>(this.base, { params });
  }

  // Crea una tarea personal
  crear(datos: TareaRequest): Observable<Tarea> {
    // Envía los datos a la API
    return this.http.post<Tarea>(this.base, datos);
  }

  // Edita una tarea personal
  actualizar(id: number, datos: TareaRequest): Observable<Tarea> {
    // Envía los datos a la API
    return this.http.put<Tarea>(`${this.base}/${id}`, datos);
  }

  // Cambia el estado y el avance de una tarea
  registrarAvance(id: number, datos: AvanceRequest): Observable<Tarea> {
    // Envía el cambio a la API
    return this.http.patch<Tarea>(`${this.base}/${id}/avance`, datos);
  }

  // Borra una tarea personal
  eliminar(id: number): Observable<void> {
    // Envía la orden a la API
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
