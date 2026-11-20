// Paquete de la capa de lógica de negocio
package com.orvian.api.service;

// Reloj que se inyecta para poder probar el paso del tiempo
import java.time.Clock;
// Intervalo de tiempo
import java.time.Duration;
// Importa la fecha y hora con precisión de instante
import java.time.Instant;
// Cola de dos extremos para guardar los momentos de cada fallo
import java.util.ArrayDeque;
// Interfaz de colas de dos extremos
import java.util.Deque;
// Mapa seguro para varios hilos
import java.util.concurrent.ConcurrentHashMap;

// Inyecta el valor de una propiedad del archivo de configuración
import org.springframework.beans.factory.annotation.Value;
// Anotación que registra la clase como componente
import org.springframework.stereotype.Component;

// Excepción por exceso de intentos
import com.orvian.api.exception.DemasiadosIntentosException;

// Frena los ataques de fuerza bruta contra el inicio de sesión; el contador vive en memoria de esta instancia
@Component
// Clase que cuenta los fallos recientes por cuenta y por dirección IP
public class LimiteIntentos {

    // Cantidad de claves a partir de la cual se limpian las entradas vencidas
    private static final int TAMANO_MAXIMO_REGISTRO = 10_000;

    // Reloj del sistema
    private final Clock reloj;
    // Fallos permitidos para una misma cuenta desde una misma IP
    private final int maximoPorCuenta;
    // Fallos permitidos desde una misma IP sumando todas las cuentas, para frenar el ataque que prueba muchos correos
    private final int maximoPorIp;
    // Tiempo durante el que cuenta cada fallo
    private final Duration ventana;
    // Momentos de los fallos recientes, por clave
    private final ConcurrentHashMap<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();

    // Spring entrega el reloj y toma los límites de la configuración, con valores por defecto
    public LimiteIntentos(Clock reloj,
            @Value("${orvian.seguridad.login.max-intentos-cuenta:5}") int maximoPorCuenta,
            @Value("${orvian.seguridad.login.max-intentos-ip:50}") int maximoPorIp,
            @Value("${orvian.seguridad.login.ventana-minutos:15}") long ventanaMinutos) {
        // Guarda el reloj
        this.reloj = reloj;
        // Guarda el límite por cuenta
        this.maximoPorCuenta = maximoPorCuenta;
        // Guarda el límite por IP
        this.maximoPorIp = maximoPorIp;
        // Guarda la ventana de tiempo
        this.ventana = Duration.ofMinutes(ventanaMinutos);
    }

    // Se llama antes de verificar la contraseña: si ya se superó el límite, rechaza sin consultar nada más
    public void exigirPermitido(String ip, String correo) {
        // Comprueba el límite de la cuenta
        comprobar(claveCuenta(ip, correo), maximoPorCuenta);
        // Comprueba el límite de la IP
        comprobar(claveIp(ip), maximoPorIp);
    }

    // Se llama cuando las credenciales fueron incorrectas
    public void registrarFallo(String ip, String correo) {
        // Momento actual
        Instant ahora = Instant.now(reloj);
        // Anota el fallo de la cuenta
        anotar(claveCuenta(ip, correo), ahora);
        // Anota el fallo de la IP
        anotar(claveIp(ip), ahora);
        // Si el registro creció demasiado, se limpian las entradas vencidas
        if (fallos.size() > TAMANO_MAXIMO_REGISTRO) {
            // Elimina las claves sin fallos recientes
            fallos.entrySet().removeIf(entrada -> {
                // Bloquea la cola mientras se limpia
                synchronized (entrada.getValue()) {
                    // Descarta los fallos vencidos
                    purgar(entrada.getValue(), ahora);
                    // Si no queda ninguno, la clave se elimina
                    return entrada.getValue().isEmpty();
                }
            });
        }
    }

    // Se llama cuando el inicio de sesión fue correcto
    public void registrarExito(String ip, String correo) {
        // Reinicia solo el contador de la cuenta; el de la IP no, para que un atacante con una cuenta válida no lo borre
        fallos.remove(claveCuenta(ip, correo));
    }

    // Rechaza si la clave ya alcanzó el máximo de fallos dentro de la ventana
    private void comprobar(String clave, int maximo) {
        // Fallos registrados para la clave
        Deque<Instant> intentos = fallos.get(clave);
        // Sin registros no hay nada que limitar
        if (intentos == null) {
            // Termina
            return;
        }
        // Momento actual
        Instant ahora = Instant.now(reloj);
        // Segundos que faltan para que el fallo más antiguo salga de la ventana
        long espera;
        // Bloquea la cola mientras se revisa
        synchronized (intentos) {
            // Descarta los fallos vencidos
            purgar(intentos, ahora);
            // Si no se alcanzó el máximo, está permitido
            if (intentos.size() < maximo) {
                // Termina
                return;
            }
            // Calcula cuánto falta para que se libere un lugar, redondeando hacia arriba a segundos completos
            espera = (Duration.between(ahora, intentos.peekFirst().plus(ventana)).toMillis() + 999) / 1000;
        }
        // Rechaza indicando la espera, que como mínimo es de un segundo
        throw new DemasiadosIntentosException(Math.max(espera, 1));
    }

    // Agrega un fallo a la clave
    private void anotar(String clave, Instant ahora) {
        // Obtiene o crea la cola de la clave
        Deque<Instant> intentos = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        // Bloquea la cola mientras se modifica
        synchronized (intentos) {
            // Descarta los fallos vencidos
            purgar(intentos, ahora);
            // Agrega el fallo nuevo
            intentos.addLast(ahora);
        }
    }

    // Descarta los fallos que ya salieron de la ventana
    private void purgar(Deque<Instant> intentos, Instant ahora) {
        // Límite a partir del cual un fallo ya no cuenta
        Instant limite = ahora.minus(ventana);
        // Los fallos están en orden, así que se retiran desde el más antiguo
        while (!intentos.isEmpty() && intentos.peekFirst().isBefore(limite)) {
            // Retira el fallo vencido
            intentos.pollFirst();
        }
    }

    // Clave de una cuenta desde una IP
    private String claveCuenta(String ip, String correo) {
        // Combina IP y correo
        return "cuenta:" + ip + "|" + correo;
    }

    // Clave de una IP
    private String claveIp(String ip) {
        // Identifica la IP
        return "ip:" + ip;
    }
}
