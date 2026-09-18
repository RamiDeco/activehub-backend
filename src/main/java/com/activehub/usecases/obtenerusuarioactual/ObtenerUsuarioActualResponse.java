package com.activehub.usecases.obtenerusuarioactual;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ObtenerUsuarioActualResponse(
        UUID id,
        String nombre,
        String apellido,
        String email,
        String dni,
        String telefono,
        LocalDate fechaNacimiento,
        String rol,
        String estado,
        int cantidadPenalizaciones,
        Instant createdAt,
        /**
         * Si confirmó su correo con el código de 6 dígitos. En false el frontend lo manda a
         * la pantalla del código: hasta confirmarlo, su correo sigue libre para otro.
         */
        boolean emailVerificado,
        /** LOCAL o GOOGLE. Una cuenta de Google no tiene contraseña ni puede cambiar su correo. */
        String authProveedor,
        /** Solo para alumnos: para instructor/admin viaja vacía. Alimenta "Recomendado para vos". */
        List<Interes> intereses,
        /** Claves habilitadas para el rol (RN-19): el frontend oculta lo que el usuario no puede. */
        List<String> permisos
) {

    /** Un interés es un TipoActividad (V19), con su categoría a cuestas. */
    public record Interes(UUID tipoActividadId, String nombre, UUID categoriaId, String categoria) {
    }
}
