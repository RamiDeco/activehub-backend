package com.activehub.usecases.denunciarresenia;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DenunciarReseniaRequest(
        @NotBlank(message = "Contanos por qué denunciás esta reseña")
        @Size(max = 1000, message = "El motivo no puede superar los 1000 caracteres") @SinHtml String motivo
) {
}
