package com.activehub.usecases.crearclase;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CrearClaseRequest(
        @NotNull(message = "La fecha y hora son obligatorias")
        @Future(message = "La fecha de la clase debe ser futura") Instant fechaHora,
        @Min(value = 1, message = "Los cupos máximos deben ser al menos 1") Integer cuposMax
) {
}
