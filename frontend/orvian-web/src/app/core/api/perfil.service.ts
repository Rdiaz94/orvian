// Utilidades de Angular
import { Injectable, inject } from '@angular/core';
// Cliente HTTP
import { HttpClient } from '@angular/common/http';
// Tipo de flujo de datos
import { Observable } from 'rxjs';

// Dirección de la API según el entorno
import { environment } from '../../../environments/environment';
// Tipos de datos
import { CambioContrasenaRequest, PerfilRequest, Usuario } from '../modelos';

// Edita el perfil del usuario autenticado
@Injectable({ providedIn: 'root' })
export class PerfilApi {
  // Cliente HTTP
  private readonly http = inject(HttpClient);
  // Dirección base del perfil
  private readonly base = `${environment.apiUrl}/api/perfil`;

  // Actualiza el nombre, la carrera y el ciclo
  actualizar(datos: PerfilRequest): Observable<Usuario> {
    // Envía los datos a la API
    return this.http.put<Usuario>(this.base, datos);
  }

  // Cambia la contraseña; exige la actual
  cambiarContrasena(datos: CambioContrasenaRequest): Observable<void> {
    // Envía los datos a la API
    return this.http.put<void>(`${this.base}/contrasena`, datos);
  }
}
