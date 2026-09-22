package com.activehub.usecases.solicitarrecuperacionpassword;

/**
 * La respuesta es <b>la misma exista o no la cuenta</b>. Ver la nota de enumeración en
 * {@link SolicitarRecuperacionPasswordService}: ninguno de los dos campos depende del correo
 * que se pidió.
 *
 * @param envioHabilitado si el backend tiene credenciales de SMTP. Con {@code false} el mail
 *                        no sale a ninguna parte y el código queda en el log del backend (es
 *                        el modo de desarrollo de {@code EmailSender}); la pantalla lo avisa
 *                        en vez de prometer un correo que no va a llegar.
 * @param ttlMin          cuántos minutos vale el código, para el cartel de la pantalla.
 */
public record SolicitarRecuperacionPasswordResponse(boolean envioHabilitado, int ttlMin) {
}
