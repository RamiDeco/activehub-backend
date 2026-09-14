package com.activehub.usecases.crearpenalizacion;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * El admin puede aplicar los dos tipos en una sola operacion (multa + suspension). Se guarda
 * una Penalizacion por cada tipo, no una fila "mixta": el enum de la base sigue teniendo dos
 * valores y el listado muestra cada sancion con sus propios datos.
 *
 * <p>Los campos condicionales (monto para Economica, fechas para Suspension) se validan en el
 * Service y no aca: dependen de que tipos vengan en la lista.
 */
public record CrearPenalizacionRequest(
        @NotNull(message = "Elegí a qué usuario penalizar") UUID usuarioId,
        @NotEmpty(message = "Elegí al menos un tipo de penalización") List<String> tipos,
        @NotBlank(message = "El motivo es obligatorio") @SinHtml String motivo,
        BigDecimal monto,
        LocalDate fechaInicio,
        LocalDate fechaFin
) {
}
