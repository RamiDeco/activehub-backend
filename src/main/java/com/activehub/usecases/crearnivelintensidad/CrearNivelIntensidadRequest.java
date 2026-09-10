package com.activehub.usecases.crearnivelintensidad;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearNivelIntensidadRequest(
        @NotBlank(message = "Este campo es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar los 60 caracteres") @SinHtml String nombre,
        @NotBlank(message = "Este campo es obligatorio")
        @Size(max = 300, message = "La descripción no puede superar los 300 caracteres") @SinHtml String descripcion
) {
}
