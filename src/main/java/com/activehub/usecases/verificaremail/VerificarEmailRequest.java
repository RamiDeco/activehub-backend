package com.activehub.usecases.verificaremail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificarEmailRequest(
        @NotBlank(message = "Ingresá el código que te enviamos")
        @Pattern(regexp = "^[0-9]{6}$", message = "El código tiene 6 dígitos") String codigo
) {
}
