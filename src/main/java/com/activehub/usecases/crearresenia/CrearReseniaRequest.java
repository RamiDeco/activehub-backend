package com.activehub.usecases.crearresenia;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearReseniaRequest(
        @NotNull(message = "El puntaje es obligatorio")
        @Min(value = 1, message = "El puntaje debe ser entre 1 y 5")
        @Max(value = 5, message = "El puntaje debe ser entre 1 y 5") Integer puntaje,
        // E3A-HU10 criterio 3: "Tu comentario (opcional)". Solo el puntaje es obligatorio.
        @SinHtml String comentario
) {
}
