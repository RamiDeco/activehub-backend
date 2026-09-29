package com.activehub.usecases.actualizarclase;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ActualizarClaseRequest(
        @NotNull(message = "La fecha y hora son obligatorias") Instant fechaHora,
        // Sin horaFin, igual que al crear: la calcula el Service con `actividad.duracionMin`.
        // La duracion es un dato de la actividad, no de cada clase.

        @NotNull(message = "Los cupos máximos son obligatorios")
        @Min(value = 1, message = "El cupo debe ser un número entero mayor a 0") Integer cuposMax
) {
}
