package com.activehub.domain.inscripcion;

public enum MetodoPago {
    MERCADO_PAGO("Mercado Pago"),
    EFECTIVO("Efectivo");

    private final String etiqueta;

    MetodoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static MetodoPago fromEtiqueta(String etiqueta) {
        for (MetodoPago metodo : values()) {
            if (metodo.etiqueta.equals(etiqueta)) {
                return metodo;
            }
        }
        throw new IllegalArgumentException("Método de pago inválido: " + etiqueta);
    }
}
