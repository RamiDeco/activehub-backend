package com.activehub.usecases.listarmisclases;

import java.time.Instant;
import java.util.UUID;

public record ListarMisClasesResponse(
        UUID claseId,
        UUID actividadId,
        String actividadNombre,
        String actividadUbicacion,
        Instant fechaHora,
        String estado,
        int cuposMax,
        int cuposOcupados
) {
}
