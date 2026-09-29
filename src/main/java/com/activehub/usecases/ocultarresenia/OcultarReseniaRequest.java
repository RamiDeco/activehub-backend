package com.activehub.usecases.ocultarresenia;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El motivo es obligatorio: ocultar el contenido de otra persona es una decision que hay que
 * poder justificar despues, y es lo que queda en auditoria.
 */
public record OcultarReseniaRequest(
        @NotBlank(message = "Indicá por qué ocultás la reseña")
        @Size(max = 300, message = "El motivo no puede superar los 300 caracteres")
        @SinHtml String motivo
) {
}
