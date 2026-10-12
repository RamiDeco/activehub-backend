package com.activehub.usecases.listarreportessoporte;

import java.time.Instant;
import java.util.UUID;

public record ListarReportesSoporteResponse(
        UUID id,
        String email,
        String asunto,
        String detalle,
        String estado,
        /** Nombre de quien lo mando, o null si lo envio un visitante sin cuenta. */
        String autorNombre,
        UUID autorId,
        String respuesta,
        String cerradoPorNombre,
        Instant cerradoAt,
        Instant createdAt
) {
}
