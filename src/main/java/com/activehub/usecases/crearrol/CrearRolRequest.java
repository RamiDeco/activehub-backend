package com.activehub.usecases.crearrol;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearRolRequest(
        @NotBlank(message = "Este campo es obligatorio")
        @Size(max = 40, message = "El nombre no puede superar los 40 caracteres") @SinHtml String nombre,
        @Size(max = 200, message = "La descripción no puede superar los 200 caracteres") @SinHtml String descripcion
) {
}
