package com.activehub.usecases.registrarinteraccion;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * @param tipo        BUSQUEDA o VISTA_ACTIVIDAD. Se valida contra el enum en el Service: que
 *                    llegue un valor desconocido es un 400, no un 500.
 * @param actividadId obligatorio en VISTA_ACTIVIDAD, ignorado en BUSQUEDA.
 * @param termino     obligatorio en BUSQUEDA, ignorado en VISTA_ACTIVIDAD. Lo escribio el
 *                    usuario, asi que lleva {@link SinHtml} como todo texto libre.
 */
public record RegistrarInteraccionRequest(
        @NotNull(message = "El tipo de interacción es obligatorio") String tipo,
        UUID actividadId,
        @Size(max = 120, message = "El término no puede superar los 120 caracteres")
        @SinHtml String termino
) {
}
