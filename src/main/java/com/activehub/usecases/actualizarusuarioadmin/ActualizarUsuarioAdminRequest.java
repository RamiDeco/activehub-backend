package com.activehub.usecases.actualizarusuarioadmin;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record ActualizarUsuarioAdminRequest(
        @NotBlank(message = "El nombre es obligatorio") @SinHtml String nombre,
        @NotBlank(message = "El apellido es obligatorio") @SinHtml String apellido,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Ingresá un correo electrónico válido") String email,
        @Pattern(regexp = "\\+?[0-9 ]*", message = "El teléfono debe contener solo números") String telefono,
        LocalDate fechaNacimiento
) {
}
