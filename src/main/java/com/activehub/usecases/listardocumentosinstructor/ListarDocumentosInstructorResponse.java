package com.activehub.usecases.listardocumentosinstructor;

import java.time.Instant;
import java.util.UUID;

public record ListarDocumentosInstructorResponse(
        UUID id,
        String nombreArchivo,
        String tipoDocumento,
        long tamanioBytes,
        Instant createdAt
) {
}
