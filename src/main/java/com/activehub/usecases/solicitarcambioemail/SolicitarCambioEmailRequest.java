package com.activehub.usecases.solicitarcambioemail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * @param password la contraseña actual. Cambiar el correo es cambiar la credencial de acceso:
 *                 con la sesión abierta en una máquina ajena, sin este campo alcanzaría un
 *                 click para quedarse con la cuenta.
 */
public record SolicitarCambioEmailRequest(
        @NotBlank(message = "El nuevo correo es obligatorio")
        @Email(message = "El email no tiene un formato válido") String email,
        @NotBlank(message = "Ingresá tu contraseña actual") String password
) {
}
