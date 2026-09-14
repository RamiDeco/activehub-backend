package com.activehub.usecases.actualizaractividad;

import java.math.BigDecimal;
import java.util.UUID;

public record ActualizarActividadResponse(
        UUID id,
        String nombre,
        String descripcion,
        TipoActividad tipoActividad,
        Categoria categoria,
        NivelIntensidad nivelIntensidad,
        Instructor instructor,
        BigDecimal precio,
        String ubicacion,
        String photoTint,
        BigDecimal rating,
        int duracionMin,
        Double latitud,
        Double longitud
) {
    public record NivelIntensidad(UUID id, String nombre) {
    }

    public record TipoActividad(UUID id, String nombre) {
    }

    public record Categoria(UUID id, String nombre) {
    }

    public record Instructor(UUID id, String nombre, String apellido) {
    }
}
