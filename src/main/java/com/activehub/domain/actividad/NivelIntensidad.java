package com.activehub.domain.actividad;

public enum NivelIntensidad {
    FISICA_BAJA("Física baja"),
    FISICA_MEDIA("Física media"),
    FISICA_ALTA("Física alta");

    private final String etiqueta;

    NivelIntensidad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static NivelIntensidad fromEtiqueta(String etiqueta) {
        for (NivelIntensidad nivel : values()) {
            if (nivel.etiqueta.equals(etiqueta)) {
                return nivel;
            }
        }
        throw new IllegalArgumentException("Nivel de intensidad inválido: " + etiqueta);
    }
}
