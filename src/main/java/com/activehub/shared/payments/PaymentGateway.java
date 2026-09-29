package com.activehub.shared.payments;

import java.math.BigDecimal;

/**
 * Pasarela de pagos del marketplace (escrow con captura diferida, decision de MP-01).
 *
 * <p>Toda operacion va con el token OAuth del <b>instructor</b> ({@code sellerToken}): la
 * orden se crea en su cuenta, no en la de la plataforma. La {@code idempotencyKey} la decide
 * el que llama y la pasa por parametro — la pasarela nunca inventa una por llamada, porque
 * entonces un reintento del mismo pedido cobraria dos veces. Mercado Pago rechaza reutilizar
 * una clave con otro body, asi que cada operacion sobre la misma orden lleva la suya.
 */
public interface PaymentGateway {

    /** Crea la orden con captura MANUAL: la plata queda reservada, no cobrada. */
    ResultadoPago autorizar(String sellerToken, DatosTarjeta tarjeta, BigDecimal monto, BigDecimal comision,
                            String externalReference, String idempotencyKey);

    ResultadoPago capturar(String sellerToken, String orderId, String idempotencyKey);

    ResultadoPago cancelarAutorizacion(String sellerToken, String orderId, String idempotencyKey);

    ResultadoPago reembolsar(String sellerToken, String orderId, String idempotencyKey);

    /** Estado actual de la orden (lo usa el webhook). */
    ResultadoPago consultar(String sellerToken, String orderId);

    /**
     * @deprecated transicion de MP-04: los casos de uso todavia no pasan el token del
     * instructor. Se reemplaza por {@link #capturar} en KAN-65.
     */
    @Deprecated
    void confirmarPago(String referenciaExterna);

    /**
     * @deprecated transicion de MP-04. Se reemplaza por {@link #cancelarAutorizacion} /
     * {@link #reembolsar} en KAN-65.
     */
    @Deprecated
    void cancelarPago(String referenciaExterna);

    /** Estado de la orden tal como lo devuelve Mercado Pago ({@code status} / {@code status_detail}). */
    record ResultadoPago(String orderId, String status, String statusDetail) {

        /** Autorizada y esperando captura. */
        public boolean autorizado() {
            return "action_required".equals(status) && "waiting_capture".equals(statusDetail);
        }
    }

    /** Lo que devuelve el Card Payment Brick al tokenizar. Nunca viaja el numero de tarjeta. */
    record DatosTarjeta(String token, String paymentMethodId, String paymentMethodType, int cuotas,
                        String emailPagador) {
    }
}
