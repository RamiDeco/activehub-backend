package com.activehub.usecases.listarinstructores;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ListarInstructoresResponse(
        UUID id,
        String nombre,
        String apellido,
        String email,
        String telefono,
        LocalDate fechaNacimiento,
        Instant createdAt,
        String especialidad,
        Integer aniosExperiencia,
        String estadoVerificacion,
        String motivoRechazo
) {
}
