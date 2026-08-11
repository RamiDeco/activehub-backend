package com.activehub.usecases.registrarinstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarInstructorResponse(String token, Usuario usuario) {

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
