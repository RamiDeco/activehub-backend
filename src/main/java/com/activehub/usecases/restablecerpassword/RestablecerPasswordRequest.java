package com.activehub.usecases.restablecerpassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * El correo viaja de nuevo porque este endpoint es público y no hay sesión de la cual sacar
 * la cuenta: el código sólo identifica un pedido dentro de un usuario, no al usuario.
 */
public record RestablecerPasswordRequest(
        @NotBlank(message = "Ingresá tu correo")
        @Email(message = "Ingresá un correo válido") String email,

        @NotBlank(message = "Ingresá el código que te enviamos")
        @Pattern(regexp = "^[0-9]{6}$", message = "El código tiene 6 dígitos") String codigo,

        // Misma politica que el registro y que cambiarmicontrasenia (RN-20).
        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número"
        ) String contraseniaNueva
) {
}
