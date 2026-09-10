package com.activehub.usecases.crearactividad;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CrearActividadRequest(
        @NotBlank(message = "El nombre es obligatorio") @SinHtml String nombre,
        @NotBlank(message = "La descripción es obligatoria") @SinHtml String descripcion,
        @NotNull(message = "El tipo de actividad es obligatorio") UUID tipoActividadId,
        // Desde V21 el nivel es una entidad con ABM propio (E4Ad-HU05), así que ya no se
        // puede validar contra una lista fija en un @Pattern: el admin puede crear niveles
        // nuevos. La existencia la verifica el servicio contra la base.
        @NotNull(message = "El nivel de intensidad es obligatorio") UUID nivelIntensidadId,
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0", message = "El precio no puede ser negativo") BigDecimal precio,
        @NotBlank(message = "La ubicación es obligatoria") @SinHtml String ubicacion,
        @NotBlank(message = "El color/imagen es obligatorio") String photoTint,
        // El cupo NO va en la Actividad (sección 2: vive en Clase y en AgendaClases).
        // Lo que la pantalla de alta pide de verdad es la duración (E2I-HU03 criterio 1).
        @Min(value = 1, message = "La duración debe ser de al menos 1 minuto") int duracionMin,
        @DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90") Double latitud,
        @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180") Double longitud
) {
}
