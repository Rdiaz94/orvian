// Utilidades de Angular
import { Injectable, inject } from '@angular/core';
// Cliente HTTP
import { HttpClient } from '@angular/common/http';
// Tipo de flujo de datos
import { Observable } from 'rxjs';

// Dirección de la API según el entorno
import { environment } from '../../../environments/environment';
// Tipo del curso
import { Curso } from '../modelos';

// Consulta los cursos
@Injectable({ providedIn: 'root' })
export class CursoApi {
  // Cliente HTTP
  private readonly http = inject(HttpClient);

  // Lista todos los cursos ordenados por nombre
  listar(): Observable<Curso[]> {
    // Consulta la API
    return this.http.get<Curso[]>(`${environment.apiUrl}/api/cursos`);
  }
}
