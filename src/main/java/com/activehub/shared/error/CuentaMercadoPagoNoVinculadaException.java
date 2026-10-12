package com.activehub.shared.error;

/**
 * Se intento cobrar con Mercado Pago una clase de un instructor sin cuenta vinculada (o con el
 * token vencido). 409 y no 502: no hay nada caido, la clase simplemente solo acepta efectivo.
 */
public class CuentaMercadoPagoNoVinculadaException extends ApiException {

    public CuentaMercadoPagoNoVinculadaException() {
        super(ApiErrorCode.MERCADOPAGO_NO_VINCULADO,
                "Esta clase solo acepta pago en efectivo: el instructor todavía no vinculó su cuenta de Mercado Pago.");
    }
}
