package com.activehub.domain.usuario;

/**
 * Con qué credencial nació la cuenta.
 *
 * <p>Importa para dos cosas: una cuenta {@link #GOOGLE} tiene un {@code passwordHash}
 * aleatorio e inutilizable (la columna es NOT NULL, pero no hay contraseña que el usuario
 * conozca), así que no puede entrar por el login normal ni cambiar su contraseña; y su correo
 * nace verificado, porque Google ya lo verificó.
 */
public enum AuthProveedor {
    LOCAL,
    GOOGLE
}
