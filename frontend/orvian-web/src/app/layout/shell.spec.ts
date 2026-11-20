// Utilidades de pruebas de Angular
import { TestBed } from '@angular/core/testing';
// Cliente HTTP de pruebas
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
// Navegación
import { provideRouter } from '@angular/router';

// Utilidades de pruebas
import { USUARIO_PRUEBA, crearToken } from '../testing/utilidades';
// Estructura que se prueba
import { ShellPage } from './shell';

// Pruebas del menú de las pantallas privadas
describe('ShellPage', () => {
  // Crea la estructura con la sesión de un usuario y devuelve su texto
  function textoDelMenu(rol: 'ESTUDIANTE' | 'ADMIN'): string {
    // Deja la sesión guardada
    sessionStorage.setItem('orvian.sesion', JSON.stringify({ token: crearToken(3600, rol), usuario: { ...USUARIO_PRUEBA, rol } }));
    // Configura Angular
    TestBed.configureTestingModule({ imports: [ShellPage], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    // Crea la estructura
    const fixture = TestBed.createComponent(ShellPage);
    // Dibuja el menú
    fixture.detectChanges();
    // Devuelve el texto
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  // Limpia la sesión de cada prueba
  afterEach(() => sessionStorage.clear());

  // Menú del estudiante
  it('no muestra la opción de administración a un estudiante', () => {
    // El estudiante ve el menú común
    const texto = textoDelMenu('ESTUDIANTE');
    // Aparece una opción común
    expect(texto).toContain('Mis tareas');
    // No aparece la administración
    expect(texto).not.toContain('Administración');
  });

  // Menú del administrador
  it('muestra la opción de administración a un administrador', () => {
    // El administrador ve la opción extra
    expect(textoDelMenu('ADMIN')).toContain('Administración');
  });
});
