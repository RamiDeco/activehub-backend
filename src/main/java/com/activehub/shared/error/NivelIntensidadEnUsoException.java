package com.activehub.shared.error;

public class NivelIntensidadEnUsoException extends ApiException {

    public NivelIntensidadEnUsoException() {
        super(ApiErrorCode.NIVEL_INTENSIDAD_EN_USO,
                "No podés eliminar este Nivel porque tiene actividades asociadas.");
    }
}
