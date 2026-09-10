package com.activehub.usecases.crearclase;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

public record CrearClaseRequest(
        @NotNull(message = "La fecha y hora son obligatorias")
        @Future(message = "La fecha y hora deben ser futuras") Instant fechaHora,
        // E2I-HU06 criterio 1: "Hora fin*" es obligatoria, igual que el inicio.
        @NotNull(message = "La hora de fin es obligatoria") Instant horaFin,
        // Obligatorio a proposito. Antes era opcional y caia a `actividad.cuposMax`, un campo
        // fantasma que el formulario llenaba con 20 fijo: ese 20 terminaba siendo el cupo real.
        @NotNull(message = "El cupo máximo es obligatorio")
        @Min(value = 1, message = "El cupo debe ser un número entero mayor a 0") Integer cuposMax,
        // E2I-HU06 criterio 6: "Repetir cada semana" genera la AgendaClases de la recurrencia.
        boolean repetirSemanalmente,
        /** Fin de la recurrencia. Null = sin fecha de corte. Solo aplica si repetirSemanalmente. */
        LocalDate repetirHasta
) {
}
