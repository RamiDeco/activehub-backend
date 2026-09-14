package com.activehub.usecases.actualizarmiperfil;

import java.time.LocalDate;
import java.util.UUID;

public record ActualizarMiPerfilResponse(
        UUID id,
        String nombre,
        String apellido,
        String email,
        String telefono,
        LocalDate fechaNacimiento
) {
}
