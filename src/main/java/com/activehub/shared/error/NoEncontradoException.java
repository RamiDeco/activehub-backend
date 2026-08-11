package com.activehub.shared.error;

public class NoEncontradoException extends ApiException {

    public NoEncontradoException(String message) {
        super(ApiErrorCode.NO_ENCONTRADO, message);
    }
}
