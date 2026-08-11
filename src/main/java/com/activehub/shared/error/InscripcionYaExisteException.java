package com.activehub.shared.error;

public class InscripcionYaExisteException extends ApiException {

    public InscripcionYaExisteException(String message) {
        super(ApiErrorCode.INSCRIPCION_YA_EXISTE, message);
    }
}
