package com.activehub.usecases.listarmisdenuncias;

import java.time.Instant;
import java.util.UUID;

public record ListarMisDenunciasResponse(
        UUID id,
        /** "CLASE" o "RESENIA". */
        String tipo,
        UUID claseId,
        Instant claseFechaHora,
        UUID actividadId,
        String actividadNombre,
        String motivo,
        String estado,
        /** Cómo la cerró el admin. Null mientras siga abierta (E3A-HU11 criterios 2 y 7). */
        String resolucion,
        String detalle,
        Instant createdAt
) {
}
