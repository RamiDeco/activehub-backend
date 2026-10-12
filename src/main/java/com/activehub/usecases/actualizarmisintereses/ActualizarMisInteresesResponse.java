package com.activehub.usecases.actualizarmisintereses;

import java.util.List;
import java.util.UUID;

public record ActualizarMisInteresesResponse(UUID usuarioId, List<Interes> intereses) {

    /** Un interes es un TipoActividad, con su categoria a cuestas. */
    public record Interes(UUID tipoActividadId, String nombre, UUID categoriaId, String categoria) {
    }
}
