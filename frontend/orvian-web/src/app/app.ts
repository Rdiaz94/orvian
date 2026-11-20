// Decorador de componentes
import { Component } from '@angular/core';
// Lugar donde se muestran las pantallas
import { RouterOutlet } from '@angular/router';

// Componente raíz: solo aloja las pantallas según la ruta
@Component({
  // Nombre de la etiqueta HTML
  selector: 'app-root',
  // Componentes que usa su plantilla
  imports: [RouterOutlet],
  // Plantilla del componente
  template: '<router-outlet />',
})
export class App {}
