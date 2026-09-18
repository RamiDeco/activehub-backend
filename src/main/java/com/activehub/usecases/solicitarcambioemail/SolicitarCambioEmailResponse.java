package com.activehub.usecases.solicitarcambioemail;

/**
 * @param email   el correo NUEVO, al que se mandó el código. El de la cuenta no cambió
 *                todavía: recién cambia cuando se ingresa el código.
 * @param enviado si el SMTP lo aceptó.
 * @param ttlMin  minutos de validez del código.
 */
public record SolicitarCambioEmailResponse(String email, boolean enviado, int ttlMin) {
}
