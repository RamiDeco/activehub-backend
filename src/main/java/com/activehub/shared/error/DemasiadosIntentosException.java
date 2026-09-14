package com.activehub.shared.error;

public class DemasiadosIntentosException extends ApiException {

    public DemasiadosIntentosException() {
        super(
                ApiErrorCode.DEMASIADOS_INTENTOS,
                "Demasiados intentos fallidos. Esperá unos minutos antes de volver a intentarlo.");
    }
}
