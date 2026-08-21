package com.activehub.shared.notificacion;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Toda notificación referida a una clase debe incluir el nombre de la
 * actividad y la fecha/hora de la clase — regla de negocio explícita, no solo
 * estilo. Centralizado acá para que cada usecase que notifique sobre una
 * clase (ausencia de profesor, cancelación, nueva clase de un favorito, etc.)
 * dé el mismo formato sin repetir el DateTimeFormatter en cada uno.
 */
public final class NotificacionMensajes {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm", new Locale("es", "AR"))
            .withZone(ZoneId.of("America/Argentina/Buenos_Aires"));

    private NotificacionMensajes() {
    }

    public static String formatFechaHora(Instant instante) {
        return FECHA_HORA.format(instante) + " hs";
    }
}
