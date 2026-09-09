package com.activehub.shared.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Bloqueo temporal tras varios intentos fallidos de login (seccion 4 de la especificacion).
 *
 * <p>El contador vive EN MEMORIA, por instancia: no hay tabla ni columna. Alcanza para el
 * despliegue actual (una sola instancia) y evita una migracion para un dato que es
 * efimero por naturaleza. Si en algun momento hay mas de una instancia detras de un balanceador,
 * esto hay que mover a Redis o a la base, porque cada instancia contaria por separado.
 *
 * <p>La clave es el email normalizado, no el usuario: asi tambien se frena la fuerza bruta
 * contra un correo que no existe, sin revelar si existe o no.
 */
@Service
public class IntentosLoginService {

    static final int MAX_INTENTOS = 5;
    static final Duration VENTANA = Duration.ofMinutes(15);
    static final Duration BLOQUEO = Duration.ofMinutes(15);

    private record Registro(int fallidos, Instant primerIntento, Instant bloqueadoHasta) {
    }

    private final Map<String, Registro> porEmail = new ConcurrentHashMap<>();
    private final Clock clock;

    public IntentosLoginService(Clock clock) {
        this.clock = clock;
    }

    private String clave(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /** true si el email esta bloqueado en este momento. */
    public boolean estaBloqueado(String email) {
        Registro r = porEmail.get(clave(email));
        return r != null && r.bloqueadoHasta() != null && clock.instant().isBefore(r.bloqueadoHasta());
    }

    /** Suma un intento fallido y, si se paso del maximo, deja el email bloqueado. */
    public void registrarFallo(String email) {
        String k = clave(email);
        Instant ahora = clock.instant();
        porEmail.compute(k, (ignorado, actual) -> {
            // Ventana vencida (o primer intento): se arranca de cero.
            if (actual == null || Duration.between(actual.primerIntento(), ahora).compareTo(VENTANA) > 0) {
                return new Registro(1, ahora, null);
            }
            int fallidos = actual.fallidos() + 1;
            Instant bloqueadoHasta = fallidos >= MAX_INTENTOS ? ahora.plus(BLOQUEO) : actual.bloqueadoHasta();
            return new Registro(fallidos, actual.primerIntento(), bloqueadoHasta);
        });
    }

    /** Login exitoso: se limpia el historial de ese email. */
    public void registrarExito(String email) {
        porEmail.remove(clave(email));
    }
}
