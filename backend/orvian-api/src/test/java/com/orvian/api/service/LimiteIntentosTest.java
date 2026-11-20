// Paquete de las pruebas de la capa de servicio
package com.orvian.api.service;

// Verificaciones legibles de AssertJ
import static org.assertj.core.api.Assertions.assertThat;
// Verificación de excepciones de AssertJ
import static org.assertj.core.api.Assertions.assertThatCode;
// Verificación de excepciones de AssertJ
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Reloj base de Java
import java.time.Clock;
// Intervalo de tiempo
import java.time.Duration;
// Importa la fecha y hora con precisión de instante
import java.time.Instant;
// Zona horaria
import java.time.ZoneId;
// Zona horaria UTC
import java.time.ZoneOffset;

// Anotación que marca un método como prueba
import org.junit.jupiter.api.Test;

// Excepción por exceso de intentos
import com.orvian.api.exception.DemasiadosIntentosException;

// Pruebas del limitador de intentos sin levantar Spring
class LimiteIntentosTest {

    // Dirección IP de las pruebas
    private static final String IP = "10.0.0.1";
    // Correo de las pruebas
    private static final String CORREO = "ana@correo.com";

    // Reloj de pruebas que avanza cuando se le indica
    private static class RelojManual extends Clock {
        // Momento actual del reloj
        private Instant ahora = Instant.parse("2026-11-01T12:00:00Z");

        // Adelanta el reloj
        void avanzar(Duration tiempo) {
            // Suma el tiempo
            ahora = ahora.plus(tiempo);
        }

        // Devuelve la zona horaria
        @Override
        public ZoneId getZone() {
            // Retorna UTC
            return ZoneOffset.UTC;
        }

        // No se usa en las pruebas
        @Override
        public Clock withZone(ZoneId zona) {
            // Retorna este mismo reloj
            return this;
        }

        // Devuelve el momento actual
        @Override
        public Instant instant() {
            // Retorna el momento
            return ahora;
        }
    }

    // Reloj compartido por cada prueba
    private final RelojManual reloj = new RelojManual();
    // Limitador con 5 fallos por cuenta, 50 por IP y ventana de 15 minutos
    private final LimiteIntentos limite = new LimiteIntentos(reloj, 5, 50, 15);

    // Registra varios fallos de la misma cuenta
    private void fallar(int veces) {
        // Repite el fallo
        for (int i = 0; i < veces; i++) {
            // Anota el fallo
            limite.registrarFallo(IP, CORREO);
        }
    }

    // Marca el método como prueba
    @Test
    // Con menos fallos que el máximo debe permitir seguir intentando
    void noBloqueaConMenosFallosQueElMaximo() {
        // Cuatro fallos
        fallar(4);

        // Todavía puede intentar
        assertThatCode(() -> limite.exigirPermitido(IP, CORREO)).doesNotThrowAnyException();
    }

    // Marca el método como prueba
    @Test
    // Al llegar al máximo debe bloquear e indicar cuánto esperar
    void bloqueaAlLlegarAlMaximoEIndicaLaEspera() {
        // Cinco fallos
        fallar(5);

        // Debe bloquear con una espera dentro de la ventana
        assertThatThrownBy(() -> limite.exigirPermitido(IP, CORREO))
                .isInstanceOf(DemasiadosIntentosException.class)
                .satisfies(error -> assertThat(((DemasiadosIntentosException) error).getSegundosDeEspera()).isBetween(1L, 900L));
    }

    // Marca el método como prueba
    @Test
    // Pasada la ventana el bloqueo debe levantarse solo
    void elBloqueoSeLevantaCuandoPasaLaVentana() {
        // Cinco fallos
        fallar(5);
        // Pasan 15 minutos y un segundo
        reloj.avanzar(Duration.ofMinutes(15).plusSeconds(1));

        // Ya puede intentar
        assertThatCode(() -> limite.exigirPermitido(IP, CORREO)).doesNotThrowAnyException();
    }

    // Marca el método como prueba
    @Test
    // Un inicio de sesión correcto debe reiniciar el contador de la cuenta
    void elExitoReiniciaElContadorDeLaCuenta() {
        // Cuatro fallos
        fallar(4);
        // Inicia sesión correctamente
        limite.registrarExito(IP, CORREO);
        // Cuatro fallos más
        fallar(4);

        // No llega al máximo porque el contador se reinició
        assertThatCode(() -> limite.exigirPermitido(IP, CORREO)).doesNotThrowAnyException();
    }

    // Marca el método como prueba
    @Test
    // Bloquear una cuenta no debe afectar a otra cuenta desde la misma IP
    void otraCuentaDesdeLaMismaIpNoSeBloquea() {
        // Cinco fallos de una cuenta
        fallar(5);

        // Otra cuenta puede intentar
        assertThatCode(() -> limite.exigirPermitido(IP, "luis@correo.com")).doesNotThrowAnyException();
    }

    // Marca el método como prueba
    @Test
    // Bloquear una cuenta desde una IP no debe afectar a la misma cuenta desde otra IP
    void otraIpNoSeBloquea() {
        // Cinco fallos desde una IP
        fallar(5);

        // La misma cuenta desde otra IP puede intentar, así un atacante no puede dejar fuera a la víctima real
        assertThatCode(() -> limite.exigirPermitido("10.0.0.2", CORREO)).doesNotThrowAnyException();
    }

    // Marca el método como prueba
    @Test
    // Probar muchos correos distintos desde una IP debe terminar bloqueando a esa IP
    void bloqueaUnaIpQueProbaMuchosCorreos() {
        // Limitador con un máximo de 10 fallos por IP
        LimiteIntentos estricto = new LimiteIntentos(reloj, 5, 10, 15);
        // Diez fallos con correos distintos
        for (int i = 0; i < 10; i++) {
            // Cada intento usa otro correo
            estricto.registrarFallo(IP, "usuario" + i + "@correo.com");
        }

        // Un correo nuevo desde esa IP también queda bloqueado
        assertThatThrownBy(() -> estricto.exigirPermitido(IP, "otro@correo.com")).isInstanceOf(DemasiadosIntentosException.class);
    }
}
