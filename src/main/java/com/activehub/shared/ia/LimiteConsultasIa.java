package com.activehub.shared.ia;

import com.activehub.shared.error.IaSinCuotaException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Cuántas consultas al modelo puede hacer un mismo cliente por minuto y por hora.
 *
 * <h2>Por qué existe</h2>
 *
 * El endpoint del asistente de ayuda es <b>público</b> (la burbuja "¿Dudas?" también vive en la
 * pantalla de Ayuda, que se usa sin sesión — es el mismo criterio que "Reportar un problema"). Sin
 * un límite, un script quema la cuota gratuita de Groq en segundos y el asistente queda caído para
 * todos los demás. No es una defensa contra el abuso en abstracto: es lo que protege una cuota
 * compartida y agotable.
 *
 * <p>Dos ventanas, porque atajan cosas distintas: la del minuto frena la ráfaga y la de la hora
 * frena al que insiste despacio todo el día.
 *
 * <h2>Rechaza diciendo CUÁNTO falta</h2>
 *
 * Lanza {@link IaSinCuotaException} (429 {@code IA_SIN_CUOTA}) con la espera calculada sobre la
 * propia ventana: cuándo se libera el cupo más viejo que todavía cuenta. Es la misma excepción con
 * la que se reporta el 429 del proveedor, así que la pantalla tiene un solo caso que atender, y el
 * mensaje siempre puede decirle a la persona cuándo volver en vez de dejarla probando a ciegas.
 *
 * <p>El contador vive <b>en memoria</b>, por instancia, igual que {@code IntentosLoginService}:
 * alcanza para el despliegue actual (una sola instancia) y no justifica una tabla para un dato
 * efímero. Con más de una instancia detrás de un balanceador, cada una contaría por separado.
 */
@Service
public class LimiteConsultasIa {

    public static final int MAX_POR_MINUTO = 6;
    public static final int MAX_POR_HORA = 40;
    private static final Duration MINUTO = Duration.ofMinutes(1);
    private static final Duration HORA = Duration.ofHours(1);

    /** Ventana deslizante: los instantes de las consultas recientes de una misma clave. */
    private final Map<String, Deque<Instant>> porClave = new ConcurrentHashMap<>();
    private final Clock clock;

    public LimiteConsultasIa(Clock clock) {
        this.clock = clock;
    }

    /**
     * Registra una consulta de esta clave, o la rechaza si se pasó del límite.
     *
     * @param clave el id del usuario logueado, o la IP si la consulta es anónima. Nunca vacía: con
     *              una clave única para todos, el primero que consulta le corta el paso al resto.
     * @throws IaSinCuotaException 429, con la espera que falta. Quien lo reciba le pone el texto que
     *                             corresponda a su pantalla ({@code paraElAsistente()} /
     *                             {@code paraElInforme()}).
     */
    public void registrar(String clave) {
        Instant ahora = clock.instant();
        Deque<Instant> recientes = porClave.computeIfAbsent(clave, k -> new ArrayDeque<>());

        // Sincronizado por clave: dos pestañas del mismo usuario pueden preguntar a la vez y la
        // Deque no es thread-safe. El lock es por cliente, así que no serializa a los demás.
        synchronized (recientes) {
            recientes.removeIf(i -> i.isBefore(ahora.minus(HORA)));
            long enElMinuto = recientes.stream().filter(i -> i.isAfter(ahora.minus(MINUTO))).count();

            if (enElMinuto >= MAX_POR_MINUTO) {
                throw sinCuota(esperaHasta(recientes, ahora, MINUTO, MAX_POR_MINUTO));
            }
            if (recientes.size() >= MAX_POR_HORA) {
                throw sinCuota(esperaHasta(recientes, ahora, HORA, MAX_POR_HORA));
            }
            recientes.addLast(ahora);
        }

        // La primera consulta de una clave nueva crea su entrada; sin esta limpieza el mapa crece
        // con una entrada por IP para siempre. Se hace acá y no con un scheduler porque el costo es
        // despreciable y así no hay un hilo más que mantener.
        if (porClave.size() > 5_000) {
            porClave.entrySet().removeIf(e -> {
                Deque<Instant> d = e.getValue();
                synchronized (d) {
                    return d.isEmpty() || d.getLast().isBefore(ahora.minus(HORA));
                }
            });
        }
    }

    private IaSinCuotaException sinCuota(Duration espera) {
        return new IaSinCuotaException(
                espera,
                IaSinCuotaException.Motivo.USO_INTENSO,
                "Hiciste muchas consultas seguidas al asistente. Esperá un momento y volvé a intentar.");
    }

    /**
     * Cuándo se libera un cupo: es cuando la consulta número {@code maximo} contando desde la más
     * vieja de la ventana sale de ella. Ese cálculo es el que permite decirle a la persona "volvé en
     * X" en vez de "esperá un rato"; sin él, el mensaje no puede prometer nada.
     */
    private Duration esperaHasta(Deque<Instant> recientes, Instant ahora, Duration ventana, int maximo) {
        Instant desde = ahora.minus(ventana);
        Instant[] enVentana = recientes.stream().filter(i -> i.isAfter(desde)).toArray(Instant[]::new);
        if (enVentana.length < maximo) {
            return ventana;
        }
        // enVentana está en orden de llegada: el que hay que esperar es el que ocupó el último cupo.
        Instant libera = enVentana[enVentana.length - maximo].plus(ventana);
        Duration falta = Duration.between(ahora, libera);
        return falta.isNegative() || falta.isZero() ? Duration.ofSeconds(1) : falta;
    }
}
