package com.activehub.shared.error;

import org.springframework.http.HttpStatus;

public enum ApiErrorCode {
    VALIDACION(HttpStatus.BAD_REQUEST),
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED),
    SIN_PERMISO(HttpStatus.FORBIDDEN),
    NO_ENCONTRADO(HttpStatus.NOT_FOUND),
    EMAIL_EN_USO(HttpStatus.CONFLICT),
    USUARIO_SUSPENDIDO(HttpStatus.FORBIDDEN),
    TIPO_ACTIVIDAD_EN_USO(HttpStatus.CONFLICT),
    CATEGORIA_EN_USO(HttpStatus.CONFLICT),
    SIN_CUPOS_DISPONIBLES(HttpStatus.CONFLICT),
    INSCRIPCION_YA_EXISTE(HttpStatus.CONFLICT),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ApiErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
