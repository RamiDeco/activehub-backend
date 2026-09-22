package com.activehub.usecases.solicitarrecuperacionpassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitarRecuperacionPasswordRequest(
        @NotBlank(message = "Ingresá tu correo")
        @Email(message = "Ingresá un correo válido") String email
) {
}
