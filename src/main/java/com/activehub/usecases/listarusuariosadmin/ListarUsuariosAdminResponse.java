package com.activehub.usecases.listarusuariosadmin;

import java.time.Instant;
import java.util.UUID;

public record ListarUsuariosAdminResponse(
        UUID id,
        String nombre,
        String apellido,
        String email,
        String dni,
        String telefono,
        String rol,
        String estado,
        int cantidadPenalizaciones,
        Instant createdAt,
        /**
         * Si su rol puede dictar clases ({@code clases.gestionar}). Se calcula por permiso y no
         * por nombre de rol (RN-19). La pantalla de Penalizaciones lo usa para ofrecer solo
         * instructores: penalizar a un alumno o a un administrador no significa nada — la
         * sancion existe por la inasistencia del profesor (E4Ad-HU06).
         */
        boolean puedeDarClases
) {
}
