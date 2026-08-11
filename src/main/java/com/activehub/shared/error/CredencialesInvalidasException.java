package com.activehub.shared.error;

public class CredencialesInvalidasException extends ApiException {

    public CredencialesInvalidasException() {
        super(ApiErrorCode.CREDENCIALES_INVALIDAS,
                "Correo o contraseña incorrectos. Verificá tus datos e intentá de nuevo.");
    }
}
