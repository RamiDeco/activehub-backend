package com.activehub.domain.usuario;

/**
 * Para qué se emitió un código. Decide el texto del mail y qué pasa al confirmarlo.
 *
 * <p>{@link #REGISTRO} confirma el correo con el que se creó la cuenta. {@link #CAMBIO_EMAIL}
 * confirma uno <b>nuevo</b>: hasta que el usuario ingresa el código, su cuenta sigue con el
 * correo viejo verificado — un error de tipeo en el nuevo no lo deja sin credencial.
 *
 * <p>{@link #RECUPERACION_PASSWORD} es el único que se emite <b>sin sesión</b>: lo pide quien
 * no puede entrar. Por eso el código que lleva no confirma ningún correo (el correo ya estaba
 * verificado, es justamente lo que permite mandárselo) sino que habilita una sola operación:
 * escribir una contraseña nueva. Quien lo consume tiene que verificar el propósito antes de
 * aplicar el cambio: un código de alta o de cambio de correo no puede servir para eso.
 */
public enum PropositoVerificacion {
    REGISTRO,
    CAMBIO_EMAIL,
    RECUPERACION_PASSWORD
}
