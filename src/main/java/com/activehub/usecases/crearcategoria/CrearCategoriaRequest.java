package com.activehub.usecases.crearcategoria;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.NotBlank;

public record CrearCategoriaRequest(
        @NotBlank(message = "El nombre es obligatorio") @SinHtml String nombre
) {
}
