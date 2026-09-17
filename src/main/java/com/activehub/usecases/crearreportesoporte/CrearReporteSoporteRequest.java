package com.activehub.usecases.crearreportesoporte;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearReporteSoporteRequest(
        @NotBlank(message = "El email de contacto es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 150, message = "El email no puede superar los 150 caracteres")
        String email,

        @NotBlank(message = "El asunto es obligatorio")
        @Size(max = 150, message = "El asunto no puede superar los 150 caracteres")
        @SinHtml
        String asunto,

        @NotBlank(message = "Contanos qué pasó")
        @Size(max = 2000, message = "El detalle no puede superar los 2000 caracteres")
        @SinHtml
        String detalle
) {
}
