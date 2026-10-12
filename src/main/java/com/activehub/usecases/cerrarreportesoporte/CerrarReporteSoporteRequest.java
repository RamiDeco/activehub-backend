package com.activehub.usecases.cerrarreportesoporte;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.Size;

public record CerrarReporteSoporteRequest(
        /** Opcional: se puede cerrar un reporte sin escribirle nada a quien lo mando. */
        @Size(max = 2000, message = "La respuesta no puede superar los 2000 caracteres")
        @SinHtml
        String respuesta
) {
}
