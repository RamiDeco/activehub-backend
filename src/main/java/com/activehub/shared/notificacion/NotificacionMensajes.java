package com.activehub.shared.notificacion;

import com.activehub.domain.inscripcion.EstadoPago;
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

    /**
     * KAN-62: que paso con la plata de un pago de Mercado Pago devuelto, para sumar al aviso del
     * alumno. Son dos cosas distintas para quien lo lee: sin capturar, nunca se le cobro (se
     * libera la reserva de la tarjeta); capturado, se le cobro y se le reembolsa.
     *
     * @param estadoFinal lo que devolvio {@code OperacionesPagoMercadoPago.reintegrar}
     */
    public static String detalleReintegro(EstadoPago estadoFinal) {
        return estadoFinal == EstadoPago.Reembolsado
                ? " Te reembolsamos el pago por Mercado Pago; según tu banco, puede tardar unos días en verse en tu tarjeta."
                : " Se liberó la reserva del pago en tu tarjeta: no se te va a cobrar.";
    }
}
