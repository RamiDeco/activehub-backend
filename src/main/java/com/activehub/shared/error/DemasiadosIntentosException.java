package com.activehub.shared.error;

public class DemasiadosIntentosException extends ApiException {

    public DemasiadosIntentosException() {
        super(
                ApiErrorCode.DEMASIADOS_INTENTOS,
                "Demasiados intentos fallidos. Esperá unos minutos antes de volver a intentarlo.");
    }

    /** Mismo 429 con otro texto: lo usa la espera mínima entre reenvíos del código de email. */
    public DemasiadosIntentosException(String message) {
        super(ApiErrorCode.DEMASIADOS_INTENTOS, message);
    }
}
