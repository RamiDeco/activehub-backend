package com.activehub.domain.penalizacion;

public enum TipoPenalizacion {
    ECONOMICA("Económica"),
    SUSPENSION_TEMPORAL("Suspensión temporal");

    private final String etiqueta;

    TipoPenalizacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static TipoPenalizacion fromEtiqueta(String etiqueta) {
        for (TipoPenalizacion tipo : values()) {
            if (tipo.etiqueta.equals(etiqueta)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de penalización inválido: " + etiqueta);
    }
}
