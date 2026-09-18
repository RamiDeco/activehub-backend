package com.activehub.usecases.actualizarmiperfil;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record ActualizarMiPerfilRequest(
        @NotBlank(message = "El nombre es obligatorio") @SinHtml String nombre,
        @NotBlank(message = "El apellido es obligatorio") @SinHtml String apellido,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Ingresá un correo electrónico válido") String email,
        // E3A-HU12 criterio 4: "El teléfono debe contener solo números".
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "\\+?[0-9 ]+", message = "El teléfono debe contener solo números") String telefono,
        LocalDate fechaNacimiento,
        /**
         * Opcional, y es la unica forma de cargarlo despues del alta: quien se registro con
         * Google nunca paso por un formulario que lo pidiera. Cuando viene, es clave de
         * unicidad de la cuenta igual que el correo.
         */
        @Pattern(regexp = "^$|^[0-9]{7,8}$", message = "El DNI debe tener 7 u 8 dígitos") String dni
) {
}
