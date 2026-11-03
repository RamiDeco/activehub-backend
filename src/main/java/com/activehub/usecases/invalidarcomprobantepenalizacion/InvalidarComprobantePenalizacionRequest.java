package com.activehub.usecases.invalidarcomprobantepenalizacion;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El motivo es obligatorio: es lo que el instructor tiene que corregir en el comprobante nuevo. */
public record InvalidarComprobantePenalizacionRequest(
        @NotBlank(message = "Indicá por qué el comprobante no es válido")
        @Size(max = 1000, message = "El motivo no puede superar los 1000 caracteres")
        @SinHtml String motivo
) {
}
