package com.activehub.domain.penalizacion;

/**
 * Estado del pago de una penalización Económica (V36). Las multas anteriores a V36 no tienen
 * estado (null) y no bloquean: nunca se le pidió a nadie que las pagara por la plataforma.
 */
public enum EstadoPagoPenalizacion {
    /** Aplicada y sin comprobante, o con el último comprobante marcado inválido. Bloquea. */
    PENDIENTE_PAGO,
    /** El instructor mandó un comprobante y espera al administrador. Sigue bloqueado. */
    EN_REVISION,
    /** El administrador aprobó el comprobante. Ya no bloquea. */
    PAGADA;

    /** Los dos estados en los que el instructor sigue bloqueado. */
    public boolean bloquea() {
        return this != PAGADA;
    }
}
