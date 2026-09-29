package com.activehub.usecases.reenviarcodigoemail;

/**
 * @param email    a qué dirección se mandó. En un cambio pendiente es el correo NUEVO, no el
 *                 de la cuenta: el frontend lo muestra para que el usuario sepa dónde mirar.
 * @param enviado  si el servidor SMTP lo aceptó. Con el mail sin configurar es false y el
 *                 código queda en el log del backend (ver `EmailSender`).
 * @param ttlMin   cuántos minutos vale el código, para el cartel de la pantalla.
 */
public record ReenviarCodigoEmailResponse(String email, boolean enviado, int ttlMin) {
}
