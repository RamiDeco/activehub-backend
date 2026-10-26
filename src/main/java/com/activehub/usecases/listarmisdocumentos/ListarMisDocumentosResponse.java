package com.activehub.usecases.listarmisdocumentos;

import java.time.Instant;
import java.util.UUID;

/**
 * Un documento vigente del instructor que llama. {@code nuevo} = subido después del último
 * rechazo: es lo que habilita "Volver a postularme".
 */
public record ListarMisDocumentosResponse(
        UUID id,
        String nombreArchivo,
        String tipoDocumento,
        long tamanioBytes,
        Instant createdAt,
        boolean nuevo
) {
}
