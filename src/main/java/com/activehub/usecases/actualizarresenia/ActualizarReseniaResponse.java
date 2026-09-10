package com.activehub.usecases.actualizarresenia;

import java.util.UUID;

public record ActualizarReseniaResponse(
        UUID id,
        UUID claseId,
        int puntaje,
        String comentario,
        boolean enModeracion
) {
}
