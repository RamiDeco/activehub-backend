package com.activehub.usecases.listardocumentosinstructor;

import java.time.Instant;
import java.util.UUID;

public record ListarDocumentosInstructorResponse(
        UUID id,
        String nombreArchivo,
        String tipoDocumento,
        long tamanioBytes,
        Instant createdAt,
        /** Subido después del último rechazo (2026-10-26): lo que el instructor mandó para corregir. */
        boolean nuevo,
        /** El instructor lo quitó: ya no forma parte de su documentación, queda como historial. */
        boolean retirado
) {
}
