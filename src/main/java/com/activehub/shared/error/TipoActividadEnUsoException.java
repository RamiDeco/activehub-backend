package com.activehub.shared.error;

public class TipoActividadEnUsoException extends ApiException {

    public TipoActividadEnUsoException() {
        super(ApiErrorCode.TIPO_ACTIVIDAD_EN_USO, "No se puede eliminar: hay actividades que usan este tipo.");
    }
}
