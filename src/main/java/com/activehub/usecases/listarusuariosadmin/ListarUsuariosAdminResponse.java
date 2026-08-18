package com.activehub.usecases.listarusuariosadmin;

import java.time.Instant;
import java.util.UUID;

public record ListarUsuariosAdminResponse(
        UUID id,
        String nombre,
        String apellido,
        String email,
        String telefono,
        String rol,
        String estado,
        int cantidadPenalizaciones,
        Instant createdAt
) {
}
