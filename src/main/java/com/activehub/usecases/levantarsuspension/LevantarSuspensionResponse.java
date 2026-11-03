package com.activehub.usecases.levantarsuspension;

import java.time.Instant;
import java.util.UUID;

/** @param multaPendiente si el instructor sigue bloqueado por una multa sin pagar. */
public record LevantarSuspensionResponse(UUID penalizacionId, Instant levantadaAt, boolean multaPendiente) {
}
