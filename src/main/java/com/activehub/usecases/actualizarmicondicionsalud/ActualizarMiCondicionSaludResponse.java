package com.activehub.usecases.actualizarmicondicionsalud;

import java.util.UUID;

/** {@code condicionSalud} en null = el alumno no informó nada. */
public record ActualizarMiCondicionSaludResponse(UUID usuarioId, String condicionSalud) {
}
