package com.activehub.usecases.crearclase;

import java.math.BigDecimal;
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
        /** Precio con el que nace la clase: el vigente de la actividad en este momento (V23). */
        BigDecimal precio,
        /** Id de la agenda si se pidió repetir; null si es una clase suelta. */
        UUID agendaClasesId
) {
}
