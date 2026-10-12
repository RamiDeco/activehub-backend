package com.activehub.domain.mercadopago;

public enum EstadoCuentaMercadoPago {
    VINCULADA,
    DESVINCULADA,
    /** El access token vencio y el refresh fallo: el instructor tiene que volver a vincular. */
    TOKEN_VENCIDO
}
