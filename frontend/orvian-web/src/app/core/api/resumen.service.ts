// Utilidades de Angular
import { Injectable, inject } from '@angular/core';
// Cliente HTTP
import { HttpClient } from '@angular/common/http';
// Tipo de flujo de datos
import { Observable } from 'rxjs';

// Dirección de la API según el entorno
import { environment } from '../../../environments/environment';
// Tipo del resumen
import { ResumenAcademico } from '../modelos';

// Consulta el resumen académico del usuario
@Injectable({ providedIn: 'root' })
export class ResumenApi {
  // Cliente HTTP
  private readonly http = inject(HttpClient);

  // Obtiene las estadísticas del usuario autenticado
  obtener(): Observable<ResumenAcademico> {
    // El usuario lo identifica el token, no un parámetro
    return this.http.get<ResumenAcademico>(`${environment.apiUrl}/api/resumen`);
  }
}
