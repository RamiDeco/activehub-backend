package com.activehub.usecases.asignarrolusuario;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AsignarRolUsuarioRequest(
        @NotNull(message = "Elegí un rol") UUID rolId
) {
}
