package com.activehub.usecases.listarrosterclase;

import java.util.List;
import java.util.UUID;

public record ListarRosterClaseResponse(
        UUID claseId,
        int cuposMax,
        int cuposOcupados,
        int cuposLibres,
        int cantidadInscripto,
        int cantidadPagoPendiente,
        int cantidadPreInscripcion,
        List<Alumno> alumnos
) {
    public record Alumno(
            UUID inscripcionId, UUID alumnoId, String nombre, String apellido, String telefono, String estado, Boolean presente
    ) {
    }
}
