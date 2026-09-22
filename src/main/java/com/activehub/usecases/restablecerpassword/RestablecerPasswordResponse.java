package com.activehub.usecases.restablecerpassword;

/**
 * Deliberadamente <b>no</b> devuelve un token: restablecer la contraseña no inicia sesión.
 * Quien acaba de elegirla tiene que escribirla una vez en el login, que es lo que confirma
 * que se la guardó (o que su gestor de contraseñas la retuvo).
 *
 * @param email el correo de la cuenta, para precargar el campo del login.
 */
public record RestablecerPasswordResponse(String email) {
}
