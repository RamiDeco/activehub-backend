package com.activehub.usecases.actualizarmisintereses;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * La lista completa de intereses: lo que no viene se borra.
 *
 * <p>Desde V19 son ids de {@code TipoActividad}, no texto: un interes es un tipo del catalogo
 * y arrastra su categoria.
 */
public record ActualizarMisInteresesRequest(
        @NotNull(message = "Enviá la lista de intereses")
        @Size(max = 20, message = "No podés elegir más de 20 intereses") List<UUID> tiposActividadId
) {
}
