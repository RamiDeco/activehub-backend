package com.activehub.usecases.cambiarmicontrasenia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarMiContraseniaRequest(
        @NotBlank(message = "Ingresá tu contraseña actual") String contraseniaActual,
        // Misma politica que el registro (RN-20): 8 caracteres, una mayuscula y un numero.
        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número"
        ) String contraseniaNueva
) {
}
