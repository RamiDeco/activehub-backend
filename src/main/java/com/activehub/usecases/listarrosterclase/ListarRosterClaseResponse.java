package com.activehub.usecases.listarrosterclase;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ListarRosterClaseResponse(
        UUID claseId,
        /** Precio congelado de esta clase (V23): es lo que pagó cada uno de estos alumnos. */
        BigDecimal precio,
        int cuposMax,
        int cuposOcupados,
        int cuposLibres,
        int cantidadInscripto,
        int cantidadPagoPendiente,
        int cantidadPreInscripcion,
        List<Alumno> alumnos
) {
    public record Alumno(
            UUID inscripcionId, UUID alumnoId, String nombre, String apellido, String telefono, String estado
    ) {
    }
}
