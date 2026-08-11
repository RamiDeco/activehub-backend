package com.activehub.shared.payments;

/**
 * Seam de pagos: interfaz estable detras de la cual hoy corre un mock y,
 * cuando el flujo de inscripciones este probado, se enchufa Mercado Pago real
 * implementando el mismo contrato. No usada todavia por ningun caso de uso.
 */
public interface PaymentGateway {

    IniciarPagoResultado iniciarPago(String referenciaExterna, long montoEnCentavos);

    void confirmarPago(String referenciaExterna);

    void cancelarPago(String referenciaExterna);

    record IniciarPagoResultado(String referenciaExterna, String urlCheckout) {
    }
}
