package com.activehub.usecases.actualizartipoactividad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ActualizarTipoActividadRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "La categoría es obligatoria") UUID categoriaId
) {
}
