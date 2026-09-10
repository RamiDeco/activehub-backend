package com.activehub.usecases.creardenuncia;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.NotBlank;

public record CrearDenunciaRequest(
        @NotBlank(message = "El motivo es obligatorio") @SinHtml String motivo
) {
}
