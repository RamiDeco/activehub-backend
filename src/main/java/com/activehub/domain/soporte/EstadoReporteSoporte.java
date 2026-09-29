package com.activehub.domain.soporte;

public enum EstadoReporteSoporte {
    ABIERTO("Abierto"),
    CERRADO("Cerrado");

    private final String etiqueta;

    EstadoReporteSoporte(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static EstadoReporteSoporte fromEtiqueta(String etiqueta) {
        for (EstadoReporteSoporte estado : values()) {
            if (estado.etiqueta.equals(etiqueta)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de reporte de soporte inválido: " + etiqueta);
    }
}
