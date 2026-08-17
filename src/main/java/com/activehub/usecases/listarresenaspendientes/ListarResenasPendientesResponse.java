package com.activehub.usecases.listarresenaspendientes;

import java.time.Instant;
import java.util.UUID;

public record ListarResenasPendientesResponse(
        UUID id,
        UUID claseId,
        Instant claseFechaHora,
        UUID actividadId,
        String actividadNombre,
        Alumno alumno,
        int puntaje,
        String comentario,
        Instant createdAt
) {
    public record Alumno(UUID id, String nombre, String apellido) {
    }
}
