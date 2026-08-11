package com.activehub.usecases.registraralumno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarAlumnoResponse(String token, Usuario usuario) {

    public record Usuario(
            UUID id,
            String nombre,
            String apellido,
            String email,
            String telefono,
            LocalDate fechaNacimiento,
            String rol,
            String estado,
            int cantidadPenalizaciones,
            Instant createdAt
    ) {
    }
}
