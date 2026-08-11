package com.activehub.usecases.registraradmin;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarAdminResponse(
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
