package com.activehub.domain.usuario;

/**
 * Para qué se emitió un código. Decide el texto del mail y qué pasa al confirmarlo.
 *
 * <p>{@link #REGISTRO} confirma el correo con el que se creó la cuenta. {@link #CAMBIO_EMAIL}
 * confirma uno <b>nuevo</b>: hasta que el usuario ingresa el código, su cuenta sigue con el
 * correo viejo verificado — un error de tipeo en el nuevo no lo deja sin credencial.
 */
public enum PropositoVerificacion {
    REGISTRO,
    CAMBIO_EMAIL
}
