package com.activehub.usecases.marcarasistencia;

import jakarta.validation.constraints.NotNull;

public record MarcarAsistenciaRequest(
        @NotNull(message = "El campo presente es obligatorio") Boolean presente
) {
}
