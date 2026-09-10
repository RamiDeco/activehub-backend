package com.activehub.usecases.crearactividad;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CrearActividadResponse(
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
        List<Clase> clases,
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

    public record Clase(UUID id, Instant fechaHora, String estado, int cuposMax, int cuposOcupados) {
    }
}
