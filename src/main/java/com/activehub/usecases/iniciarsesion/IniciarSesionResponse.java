package com.activehub.usecases.iniciarsesion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record IniciarSesionResponse(String token, Usuario usuario) {

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
            Instant createdAt,
            /** Ver `ObtenerUsuarioActualResponse.emailVerificado`. */
            boolean emailVerificado,
            String authProveedor
    ) {
    }
}
