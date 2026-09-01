package com.activehub.usecases.actualizaractividad;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

public record ActualizarActividadRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "La descripción es obligatoria") String descripcion,
        @NotNull(message = "El tipo de actividad es obligatorio") UUID tipoActividadId,
        @NotBlank(message = "El nivel de intensidad es obligatorio")
        @Pattern(regexp = "Física baja|Física media|Física alta", message = "Nivel de intensidad inválido")
        String nivelIntensidad,
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0", message = "El precio no puede ser negativo") BigDecimal precio,
        @NotBlank(message = "La ubicación es obligatoria") String ubicacion,
        @NotBlank(message = "El color/imagen es obligatorio") String photoTint,
        @Min(value = 1, message = "Los cupos máximos deben ser al menos 1") int cuposMax,
        @DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90") Double latitud,
        @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180") Double longitud
) {
}
