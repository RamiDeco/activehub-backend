package com.activehub.domain.inscripcion;

public enum EstadoPago {
    /**
     * Autorizado en Mercado Pago pero todavia NO capturado (captura diferida, Orders API). La
     * plata esta reservada en el medio del alumno; se captura al pasar la clase a Finalizada.
     * La autorizacion vence (ver {@code Pago.autorizadoHasta}).
     */
    Retenido,
    /** Capturado y liquidado al instructor. */
    Liberado,
    Cancelado,
    Efectivo,
    /** Mercado Pago rechazo el cobro; el motivo viaja en {@code Pago.statusDetail}. */
    Rechazado,
    /** Fue capturado y despues devuelto al alumno (refund), p. ej. por una denuncia que prospero. */
    Reembolsado
}
