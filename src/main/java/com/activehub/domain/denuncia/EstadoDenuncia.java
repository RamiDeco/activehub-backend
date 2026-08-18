package com.activehub.domain.denuncia;

public enum EstadoDenuncia {
    PENDIENTE("Pendiente"),
    EN_AUDITORIA("En Auditoría"),
    RESUELTA("Resuelta");

    private final String etiqueta;

    EstadoDenuncia(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static EstadoDenuncia fromEtiqueta(String etiqueta) {
        for (EstadoDenuncia estado : values()) {
            if (estado.etiqueta.equals(etiqueta)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de denuncia inválido: " + etiqueta);
    }
}
