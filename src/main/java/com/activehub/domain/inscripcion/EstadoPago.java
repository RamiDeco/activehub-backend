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
    Reembolsado,
    /**
     * Mercado Pago todavia no decidio (antifraude o revision manual; status {@code processing} /
     * {@code in_review}). La orden existe en MP; el webhook (KAN-60) la resuelve. Mientras tanto
     * la inscripcion queda en PagoPendiente y el instructor NO puede confirmarla a mano.
     */
    EnRevision;

    /**
     * Hay una orden viva en Mercado Pago que hay que deshacer si la inscripcion se cae
     * (cancelacion, clase cancelada, denuncia): reservada ({@code Retenido}, capturada o no) o
     * todavia en revision. Es la condicion que usan los casos de uso antes de reintegrar.
     */
    public boolean reservadoEnMercadoPago() {
        return this == Retenido || this == EnRevision;
    }
}
