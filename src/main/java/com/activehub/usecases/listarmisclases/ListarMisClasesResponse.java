package com.activehub.usecases.listarmisclases;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ListarMisClasesResponse(
        UUID claseId,
        UUID actividadId,
        String actividadNombre,
        String actividadUbicacion,
        Instant fechaHora,
        Instant horaFin,
        String estado,
        int cuposMax,
        int cuposOcupados,
        /**
         * Precio de ESTA clase (V23), no el actual de la actividad. Con el, el Historial del
         * instructor puede mostrar la ganancia real de cada clase dictada: si la actividad
         * cambio de precio despues, la clase vieja sigue valiendo lo que se cobro.
         */
        BigDecimal precio
) {
}
