package com.activehub.usecases.listardenunciasadmin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ListarDenunciasAdminResponse(
        UUID id,
        /** "CLASE" o "RESENIA": decide qué acciones de resolución ofrece la pantalla. */
        String tipo,
        UUID claseId,
        Instant claseFechaHora,
        UUID actividadId,
        String actividadNombre,
        /** El alumno de la clase denunciada. Null si la denuncia es sobre una reseña. */
        Persona alumno,
        Persona instructor,
        Persona denunciante,
        /** Datos de la reseña denunciada. Null si la denuncia es sobre una clase. */
        Resenia resenia,
        String motivo,
        String estado,
        String resolucion,
        String detalle,
        Pago pago,
        Instant createdAt
) {
    public record Persona(UUID id, String nombre, String apellido) {
    }

    public record Resenia(UUID id, int puntaje, String comentario, Persona autor, boolean oculta) {
    }

    public record Pago(UUID id, String estado, BigDecimal monto, String metodo) {
    }
}
