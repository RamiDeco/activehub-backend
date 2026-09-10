package com.activehub.usecases.crearclase;

import java.time.Instant;
import java.util.UUID;

public record CrearClaseResponse(
        UUID id,
        UUID actividadId,
        Instant fechaHora,
        Instant horaFin,
        String estado,
        int cuposMax,
        int cuposOcupados,
        /** Id de la agenda si se pidió repetir; null si es una clase suelta. */
        UUID agendaClasesId
) {
}
