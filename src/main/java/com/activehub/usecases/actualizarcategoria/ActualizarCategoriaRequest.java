package com.activehub.usecases.actualizarcategoria;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.NotBlank;

public record ActualizarCategoriaRequest(
        @NotBlank(message = "El nombre es obligatorio") @SinHtml String nombre
) {
}
