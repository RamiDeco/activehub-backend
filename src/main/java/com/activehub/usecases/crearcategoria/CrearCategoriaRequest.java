package com.activehub.usecases.crearcategoria;

import jakarta.validation.constraints.NotBlank;

public record CrearCategoriaRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre
) {
}
