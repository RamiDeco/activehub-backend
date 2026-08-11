package com.activehub.domain.inscripcion;

public enum EstadoInscripcion {
    PRE_INSCRIPCION("PreInscripción"),
    PAGO_PENDIENTE("PagoPendiente"),
    INSCRIPTO("Inscripto"),
    CANCELADA("Cancelada");

    private final String etiqueta;

    EstadoInscripcion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static EstadoInscripcion fromEtiqueta(String etiqueta) {
        for (EstadoInscripcion estado : values()) {
            if (estado.etiqueta.equals(etiqueta)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de inscripción inválido: " + etiqueta);
    }
}
