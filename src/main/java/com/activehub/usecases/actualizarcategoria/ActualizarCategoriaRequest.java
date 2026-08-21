package com.activehub.usecases.actualizarcategoria;

import jakarta.validation.constraints.NotBlank;

public record ActualizarCategoriaRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre
) {
}
