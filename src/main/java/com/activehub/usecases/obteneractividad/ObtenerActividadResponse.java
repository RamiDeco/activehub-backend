package com.activehub.usecases.obteneractividad;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ObtenerActividadResponse(
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
        /** Galería completa (sección 2: `imagenes[]`). La portada sigue siendo `fotoPath`. */
        List<UUID> imagenes,
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

    public record Clase(
            UUID id, Instant fechaHora, Instant horaFin, String estado, int cuposMax, int cuposOcupados,
            int cantidadPreInscripcion,
            /**
             * Precio de ESTA clase (V23), que es el que va a pagar quien se anote — no el de la
             * actividad, que pudo haber cambiado después de que la clase se congelara. El panel
             * de inscripción del alumno mostraba el de la actividad y prometía un importe
             * distinto del que después cobraba {@code inscribirse}.
             */
            BigDecimal precio) {
    }
}
