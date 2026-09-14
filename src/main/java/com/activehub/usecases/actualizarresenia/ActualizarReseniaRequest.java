package com.activehub.usecases.actualizarresenia;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarReseniaRequest(
        @NotNull(message = "Seleccioná una calificación")
        @Min(value = 1, message = "La calificación va de 1 a 5")
        @Max(value = 5, message = "La calificación va de 1 a 5") Integer puntaje,
        // Opcional, igual que al crear (E3A-HU10 criterio 3).
        @SinHtml String comentario
) {
}
