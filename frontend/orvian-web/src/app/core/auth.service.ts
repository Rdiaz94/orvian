// Decoradores y utilidades de inyección de Angular
import { Injectable, computed, inject, signal } from '@angular/core';
// Cliente HTTP
import { HttpClient } from '@angular/common/http';
// Tipos de flujos de datos
import { Observable, tap } from 'rxjs';

// Dirección de la API según el entorno
import { environment } from '../../environments/environment';
// Lectura de tokens
import { tokenVigente } from './jwt';
// Tipos de datos de la API
import { AuthResponse, LoginRequest, RegistroRequest, Usuario } from './modelos';

// Datos que se guardan de la sesión
interface Sesion {
  // Token JWT
  token: string;
  // Datos públicos del usuario
  usuario: Usuario;
}

// Clave con la que se guarda la sesión en el navegador
const CLAVE_SESION = 'orvian.sesion';

// Servicio que administra el inicio y el cierre de sesión
@Injectable({ providedIn: 'root' })
export class AuthService {
  // Cliente HTTP
  private readonly http = inject(HttpClient);
  // Sesión actual; es nula si no hay sesión iniciada
  private readonly sesion = signal<Sesion | null>(this.restaurar());

  // Usuario autenticado, o nulo
  readonly usuario = computed(() => this.sesion()?.usuario ?? null);
  // Token de la sesión, o nulo
  readonly token = computed(() => this.sesion()?.token ?? null);
  // Indica si hay una sesión iniciada
  readonly autenticado = computed(() => this.sesion() !== null);
  // Indica si el usuario es administrador; solo sirve para mostrar u ocultar opciones, el servidor siempre valida
  readonly esAdmin = computed(() => this.sesion()?.usuario.rol === 'ADMIN');

  // Inicia sesión y guarda el token
  login(datos: LoginRequest): Observable<AuthResponse> {
    // Envía las credenciales a la API
    return this.http.post<AuthResponse>(`${environment.apiUrl}/api/auth/login`, datos).pipe(
      // Al responder bien, guarda la sesión
      tap((respuesta) => this.guardar({ token: respuesta.token, usuario: respuesta.usuario })),
    );
  }

  // Registra un usuario nuevo
  registro(datos: RegistroRequest): Observable<Usuario> {
    // Envía los datos a la API
    return this.http.post<Usuario>(`${environment.apiUrl}/api/auth/registro`, datos);
  }

  // Actualiza los datos del usuario de la sesión, por ejemplo después de editar su perfil
  actualizarUsuario(usuario: Usuario): void {
    // Sesión actual
    const actual = this.sesion();
    // Sin sesión no hay nada que actualizar
    if (actual === null) {
      // Termina
      return;
    }
    // Conserva el mismo token con los datos nuevos
    this.guardar({ token: actual.token, usuario });
  }

  // Cierra la sesión y borra el token del navegador
  cerrarSesion(): void {
    // Borra la sesión guardada
    this.borrarGuardada();
    // Deja la sesión vacía
    this.sesion.set(null);
  }

  // Comprueba que la sesión exista y que el token no haya expirado; si expiró, la cierra
  sesionVigente(): boolean {
    // Token actual
    const token = this.token();
    // Sin token no hay sesión
    if (token === null) {
      // No hay sesión
      return false;
    }
    // Si el token expiró, se cierra la sesión
    if (!tokenVigente(token)) {
      // Cierra la sesión vencida
      this.cerrarSesion();
      // No hay sesión
      return false;
    }
    // La sesión es válida
    return true;
  }

  // Guarda la sesión en memoria y en el almacenamiento de la pestaña, que se borra al cerrarla
  private guardar(sesion: Sesion): void {
    // Intenta guardar en el navegador
    try {
      // Guarda la sesión como texto
      sessionStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
    } catch {
      // Si el navegador no permite guardar, la sesión vive solo en memoria
    }
    // Actualiza la sesión en memoria
    this.sesion.set(sesion);
  }

  // Recupera la sesión guardada si todavía es válida
  private restaurar(): Sesion | null {
    // Intenta leer del navegador
    try {
      // Lee el texto guardado
      const texto = sessionStorage.getItem(CLAVE_SESION);
      // Sin texto no hay sesión
      if (texto === null) {
        // No hay sesión guardada
        return null;
      }
      // Interpreta el texto
      const sesion = JSON.parse(texto) as Sesion;
      // Solo se restaura si el token sigue vigente
      if (typeof sesion.token === 'string' && tokenVigente(sesion.token)) {
        // Devuelve la sesión
        return sesion;
      }
    } catch {
      // Un texto dañado se trata como si no hubiera sesión
    }
    // Borra lo que haya quedado inservible
    this.borrarGuardada();
    // No hay sesión válida
    return null;
  }

  // Borra la sesión guardada en el navegador
  private borrarGuardada(): void {
    // Intenta borrar del navegador
    try {
      // Elimina la clave
      sessionStorage.removeItem(CLAVE_SESION);
    } catch {
      // Si el navegador no lo permite, no hay nada que borrar
    }
  }
}
