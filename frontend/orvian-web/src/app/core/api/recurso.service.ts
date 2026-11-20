// Utilidades de Angular
import { Injectable, inject } from '@angular/core';
// Cliente HTTP y parámetros de URL
import { HttpClient, HttpParams } from '@angular/common/http';
// Tipo de flujo de datos
import { Observable } from 'rxjs';

// Dirección de la API según el entorno
import { environment } from '../../../environments/environment';
// Tipos de datos
import { DescargaResponse, OrdenRecurso, Pagina, Recurso, RecursoRequest, TipoRecurso } from '../modelos';

// Filtros que admite la biblioteca
export interface FiltroRecursos {
  // Tipo de recurso
  tipo?: TipoRecurso;
  // Texto a buscar en el título
  texto?: string;
  // Orden de los resultados
  orden?: OrdenRecurso;
}

// Consulta la biblioteca de recursos
@Injectable({ providedIn: 'root' })
export class RecursoApi {
  // Cliente HTTP
  private readonly http = inject(HttpClient);
  // Dirección base de los recursos
  private readonly base = `${environment.apiUrl}/api/recursos`;

  // Busca recursos visibles para el usuario
  listar(filtro: FiltroRecursos = {}, pagina = 0, tamano = 20): Observable<Pagina<Recurso>> {
    // Parámetros de paginación
    let params = new HttpParams().set('pagina', pagina).set('tamano', tamano);
    // Si hay un tipo, se agrega al filtro
    if (filtro.tipo) {
      // Agrega el tipo
      params = params.set('tipo', filtro.tipo);
    }
    // Si hay texto, se agrega al filtro con el nombre que espera la API
    if (filtro.texto) {
      // Agrega el texto
      params = params.set('q', filtro.texto);
    }
    // Si hay un orden, se agrega
    if (filtro.orden) {
      // Agrega el orden
      params = params.set('orden', filtro.orden);
    }
    // Consulta la API
    return this.http.get<Pagina<Recurso>>(this.base, { params });
  }

  // Comparte un recurso
  crear(datos: RecursoRequest): Observable<Recurso> {
    // Envía los datos a la API
    return this.http.post<Recurso>(this.base, datos);
  }

  // Edita un recurso propio; el grupo no se puede cambiar
  actualizar(id: number, datos: RecursoRequest): Observable<Recurso> {
    // Envía los datos a la API
    return this.http.put<Recurso>(`${this.base}/${id}`, datos);
  }

  // Suma una descarga y devuelve el enlace que se debe abrir
  registrarDescarga(id: number): Observable<DescargaResponse> {
    // Envía la orden a la API
    return this.http.post<DescargaResponse>(`${this.base}/${id}/descarga`, null);
  }

  // Borra un recurso
  eliminar(id: number): Observable<void> {
    // Envía la orden a la API
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
