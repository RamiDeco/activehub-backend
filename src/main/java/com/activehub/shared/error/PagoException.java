package com.activehub.shared.error;

/**
 * Thrown when payment processing or communication with a payment gateway fails.
 */
public class PagoException extends ApiException {

    public PagoException(String message) {
        super(ApiErrorCode.ERROR_PAGO, message);
    }
}
