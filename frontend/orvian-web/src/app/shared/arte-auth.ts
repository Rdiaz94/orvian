// Utilidades de Angular
import { Component, input } from '@angular/core';

// Panel decorativo de las pantallas de acceso: ilustración de marca, mensaje de bienvenida y una tarjeta informativa
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-arte-auth',
  // Archivo de la plantilla
  templateUrl: './arte-auth.html',
  // Archivo de los estilos
  styleUrl: './arte-auth.scss',
})
export class ArteAuth {
  // Título de bienvenida
  readonly titulo = input.required<string>();
  // Texto que acompaña al título
  readonly texto = input.required<string>();
  // Título de la tarjeta inferior
  readonly tarjetaTitulo = input.required<string>();
  // Texto de la tarjeta inferior
  readonly tarjetaTexto = input.required<string>();
}
