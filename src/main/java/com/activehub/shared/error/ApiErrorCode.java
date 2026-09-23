package com.activehub.shared.error;

import org.springframework.http.HttpStatus;

public enum ApiErrorCode {
    VALIDACION(HttpStatus.BAD_REQUEST),
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED),
    SIN_PERMISO(HttpStatus.FORBIDDEN),
    NO_ENCONTRADO(HttpStatus.NOT_FOUND),
    EMAIL_EN_USO(HttpStatus.CONFLICT),
    DNI_EN_USO(HttpStatus.CONFLICT),
    USUARIO_SUSPENDIDO(HttpStatus.FORBIDDEN),
    TIPO_ACTIVIDAD_EN_USO(HttpStatus.CONFLICT),
    CATEGORIA_EN_USO(HttpStatus.CONFLICT),
    NIVEL_INTENSIDAD_EN_USO(HttpStatus.CONFLICT),
    SIN_CUPOS_DISPONIBLES(HttpStatus.CONFLICT),
    CLASE_CON_INSCRIPTOS(HttpStatus.CONFLICT),
    ACTIVIDAD_CON_INSCRIPTOS(HttpStatus.CONFLICT),
    DEMASIADOS_INTENTOS(HttpStatus.TOO_MANY_REQUESTS),
    /** La cuenta existe pero no confirmo su correo: no puede hacer nada mas que confirmarlo. */
    EMAIL_SIN_VERIFICAR(HttpStatus.FORBIDDEN),
    INSCRIPCION_YA_EXISTE(HttpStatus.CONFLICT),
    /**
     * El modelo de lenguaje no esta configurado o no respondio. 503 y no 500: no hay nada roto
     * adentro, hay un servicio externo que ahora no esta, y el frontend lo usa como senal para
     * caer a su comportamiento sin IA.
     */
    IA_NO_DISPONIBLE(HttpStatus.SERVICE_UNAVAILABLE),
    /**
     * El asistente se quedo sin cuota: se agoto el plan gratuito del proveedor o el usuario hizo
     * demasiadas consultas seguidas. Es 429 y NO 503 porque la diferencia le importa al usuario:
     * el 503 es "no hay IA" (y el frontend responde con las preguntas frecuentes en silencio),
     * mientras que esto es "hay IA pero tenes que esperar", y el mensaje dice cuanto.
     */
    IA_SIN_CUOTA(HttpStatus.TOO_MANY_REQUESTS),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ApiErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
