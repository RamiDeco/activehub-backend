package com.activehub.usecases.actualizarusuarioadmin;

import java.time.LocalDate;
import java.util.UUID;

public record ActualizarUsuarioAdminResponse(
        UUID id,
        String nombre,
        String apellido,
        String email,
        String telefono,
        LocalDate fechaNacimiento
) {
}
