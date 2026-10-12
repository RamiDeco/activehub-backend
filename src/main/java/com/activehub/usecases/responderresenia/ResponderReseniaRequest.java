package com.activehub.usecases.responderresenia;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResponderReseniaRequest(
        @NotBlank(message = "La respuesta es obligatoria")
        @Size(max = 1000, message = "La respuesta no puede superar los 1000 caracteres") @SinHtml String respuesta
) {
}
