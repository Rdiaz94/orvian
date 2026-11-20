// Utilidades de Angular
import { Injectable, inject } from '@angular/core';
// Cliente HTTP y parámetros de URL
import { HttpClient, HttpParams } from '@angular/common/http';
// Tipo de flujo de datos
import { Observable } from 'rxjs';

// Dirección de la API según el entorno
import { environment } from '../../../environments/environment';
// Tipos de datos
import { Grupo, GrupoDetalle, GrupoRequest, Integrante, Pagina } from '../modelos';

// Consulta los grupos del usuario
@Injectable({ providedIn: 'root' })
export class GrupoApi {
  // Cliente HTTP
  private readonly http = inject(HttpClient);
  // Dirección base de los grupos
  private readonly base = `${environment.apiUrl}/api/grupos`;

  // Lista los grupos a los que pertenece el usuario
  listar(pagina = 0, tamano = 20): Observable<Pagina<Grupo>> {
    // Parámetros de paginación
    const params = new HttpParams().set('pagina', pagina).set('tamano', tamano);
    // Consulta la API
    return this.http.get<Pagina<Grupo>>(this.base, { params });
  }

  // Crea un grupo; quien lo crea queda como líder
  crear(datos: GrupoRequest): Observable<Grupo> {
    // Envía los datos a la API
    return this.http.post<Grupo>(this.base, datos);
  }

  // Obtiene el grupo con sus integrantes; solo responde a los integrantes
  obtener(id: number): Observable<GrupoDetalle> {
    // Consulta la API
    return this.http.get<GrupoDetalle>(`${this.base}/${id}`);
  }

  // Invita a una persona por su correo; solo lo puede hacer el líder
  agregarIntegrante(id: number, correo: string): Observable<Integrante> {
    // Envía el correo a la API
    return this.http.post<Integrante>(`${this.base}/${id}/integrantes`, { correo });
  }

  // Quita a un integrante o abandona el grupo cuando se indica el propio identificador
  quitarIntegrante(id: number, usuarioId: number): Observable<void> {
    // Envía la orden a la API
    return this.http.delete<void>(`${this.base}/${id}/integrantes/${usuarioId}`);
  }

  // Elimina el grupo con sus tareas y recursos; solo lo puede hacer el líder
  eliminar(id: number): Observable<void> {
    // Envía la orden a la API
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
