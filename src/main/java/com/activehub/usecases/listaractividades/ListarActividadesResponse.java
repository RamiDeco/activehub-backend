package com.activehub.usecases.listaractividades;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ListarActividadesResponse(
        UUID id,
        String nombre,
        TipoActividad tipoActividad,
        Categoria categoria,
        String nivelIntensidad,
        Instructor instructor,
        BigDecimal precio,
        String ubicacion,
        String photoTint,
        BigDecimal rating,
        int cuposMax,
        ProximaClase proximaClase,
        Double latitud,
        Double longitud
) {
    public record TipoActividad(UUID id, String nombre) {
    }

    public record Categoria(UUID id, String nombre) {
    }

    public record Instructor(UUID id, String nombre, String apellido) {
    }

    public record ProximaClase(Instant fechaHora, String estado, int cuposMax, int cuposOcupados) {
    }
}
