package com.activehub.usecases.actualizarestadousuario;

import jakarta.validation.constraints.NotBlank;

public record ActualizarEstadoUsuarioRequest(
        @NotBlank(message = "El estado es obligatorio") String estado
) {
}
