// Utilidades de Angular
import { Injectable, inject } from '@angular/core';
// Cliente HTTP y parámetros de URL
import { HttpClient, HttpParams } from '@angular/common/http';
// Tipo de flujo de datos
import { Observable } from 'rxjs';

// Dirección de la API según el entorno
import { environment } from '../../../environments/environment';
// Tipos de datos
import { Curso, Pagina, Usuario } from '../modelos';

// Operaciones reservadas a los administradores; el servidor rechaza con 403 a quien no lo sea
@Injectable({ providedIn: 'root' })
export class AdminApi {
  // Cliente HTTP
  private readonly http = inject(HttpClient);
  // Dirección base de la administración
  private readonly base = `${environment.apiUrl}/api/admin`;

  // Lista los usuarios con paginación
  listarUsuarios(pagina = 0, tamano = 20): Observable<Pagina<Usuario>> {
    // Parámetros de paginación
    const params = new HttpParams().set('pagina', pagina).set('tamano', tamano);
    // Consulta la API
    return this.http.get<Pagina<Usuario>>(`${this.base}/usuarios`, { params });
  }

  // Crea un curso
  crearCurso(nombre: string): Observable<Curso> {
    // Envía el nombre a la API
    return this.http.post<Curso>(`${this.base}/cursos`, { nombre });
  }
}
