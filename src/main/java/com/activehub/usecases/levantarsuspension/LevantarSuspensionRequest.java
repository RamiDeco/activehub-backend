package com.activehub.usecases.levantarsuspension;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El motivo es obligatorio: levantar una sancion antes de tiempo se tiene que poder justificar. */
public record LevantarSuspensionRequest(
        @NotBlank(message = "Indicá por qué levantás la suspensión")
        @Size(max = 1000, message = "El motivo no puede superar los 1000 caracteres")
        @SinHtml String motivo
) {
}
