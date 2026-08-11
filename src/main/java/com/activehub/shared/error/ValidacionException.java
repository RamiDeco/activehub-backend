package com.activehub.shared.error;

import java.util.Map;

/**
 * Para reglas de negocio (no de forma) que fallan en el Service, no cubiertas
 * por Bean Validation en el Request DTO — ej. "cuposMax no puede ser menor a
 * los cupos ya ocupados".
 */
public class ValidacionException extends ApiException {

    public ValidacionException(String message) {
        super(ApiErrorCode.VALIDACION, message);
    }

    public ValidacionException(String message, Map<String, String> fieldErrors) {
        super(ApiErrorCode.VALIDACION, message, fieldErrors);
    }
}
