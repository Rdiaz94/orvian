// Utilidades de Angular
import { Component, input } from '@angular/core';

// Logo de Orvian: un anillo que representa la órbita de un equipo y tres nodos que son los integrantes conectados
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-logo',
  // Archivo de la plantilla
  templateUrl: './logo-orvian.html',
  // Archivo de los estilos
  styleUrl: './logo-orvian.scss',
})
export class LogoOrvian {
  // Tamaño del ícono en píxeles
  readonly tamano = input(44);
  // Indica si se muestra el nombre junto al ícono
  readonly texto = input(true);
}
