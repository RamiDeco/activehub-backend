package com.activehub.usecases.listardenunciasadmin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ListarDenunciasAdminResponse(
        UUID id,
        UUID claseId,
        Instant claseFechaHora,
        UUID actividadId,
        String actividadNombre,
        Alumno alumno,
        Instructor instructor,
        String motivo,
        String estado,
        Pago pago,
        Instant createdAt
) {
    public record Alumno(UUID id, String nombre, String apellido) {
    }

    public record Instructor(UUID id, String nombre, String apellido) {
    }

    public record Pago(UUID id, String estado, BigDecimal monto, String metodo) {
    }
}
