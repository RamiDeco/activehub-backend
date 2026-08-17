package com.activehub.usecases.listarresenasactividad;

import java.time.Instant;
import java.util.UUID;

public record ListarResenasActividadResponse(
        UUID id, UUID claseId, Alumno alumno, int puntaje, String comentario, Instant createdAt
) {
    public record Alumno(UUID id, String nombre, String apellido) {
    }
}
