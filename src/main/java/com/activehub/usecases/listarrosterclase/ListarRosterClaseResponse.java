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
    /**
     * KAN-63: el pago de cada alumno, para que el instructor vea en que estado esta su plata.
     * Los cuatro campos vienen en null (y {@code pagoCapturado} en false) si la inscripcion no
     * tiene pago.
     *
     * @param pagoNeto lo que va a recibir el instructor por este alumno: el monto menos la
     *                 comision de la plataforma (hoy 0, decision de MP-01)
     */
    public record Alumno(
            UUID inscripcionId, UUID alumnoId, String nombre, String apellido, String telefono, String estado,
            String pagoEstado, String pagoMetodo, boolean pagoCapturado, BigDecimal pagoNeto
    ) {
    }
}
