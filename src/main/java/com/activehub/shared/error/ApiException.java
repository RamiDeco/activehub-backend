package com.activehub.shared.error;

import java.util.Map;
import lombok.Getter;

@Getter
public abstract class ApiException extends RuntimeException {

    private final ApiErrorCode code;
    private final Map<String, String> fieldErrors;

    protected ApiException(ApiErrorCode code, String message) {
        this(code, message, null);
    }

    protected ApiException(ApiErrorCode code, String message, Map<String, String> fieldErrors) {
        super(message);
        this.code = code;
        this.fieldErrors = fieldErrors;
    }
}
