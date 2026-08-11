package com.activehub.shared.error;

public class UsuarioSuspendidoException extends ApiException {

    public UsuarioSuspendidoException() {
        super(ApiErrorCode.USUARIO_SUSPENDIDO, "Tu cuenta está suspendida. Contactá a soporte.");
    }
}
