package com.activehub.usecases.creartipoactividad;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CrearTipoActividadRequest(
        @NotBlank(message = "El nombre es obligatorio") @SinHtml String nombre,
        @NotNull(message = "La categoría es obligatoria") UUID categoriaId
) {
}
