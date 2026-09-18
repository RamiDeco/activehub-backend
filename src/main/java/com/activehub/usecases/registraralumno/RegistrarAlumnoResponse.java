package com.activehub.usecases.registraralumno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @param mailEnviado si el código de verificación salió por SMTP. En `false` la cuenta se
 *                    creó igual (el alta no depende del mail) y el frontend ofrece reenviar;
 *                    con el mail sin configurar, el código está en el log del backend.
 */
public record RegistrarAlumnoResponse(String token, boolean mailEnviado, Usuario usuario) {

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
