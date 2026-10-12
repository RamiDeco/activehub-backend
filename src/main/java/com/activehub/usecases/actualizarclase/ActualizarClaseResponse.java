package com.activehub.usecases.actualizarclase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ActualizarClaseResponse(
        UUID id, UUID actividadId, Instant fechaHora, Instant horaFin, String estado, int cuposMax, int cuposOcupados,
        /** Precio de la clase (V23). Editar la clase no lo toca: sólo lo propaga `actualizaractividad`. */
        BigDecimal precio) {
}
