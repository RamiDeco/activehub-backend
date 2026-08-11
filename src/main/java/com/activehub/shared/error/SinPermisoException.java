package com.activehub.shared.error;

public class SinPermisoException extends ApiException {

    public SinPermisoException(String message) {
        super(ApiErrorCode.SIN_PERMISO, message);
    }
}
