package com.activehub.shared.error;

import java.util.Map;

public class TipoActividadDuplicadoException extends ApiException {

    public TipoActividadDuplicadoException() {
        super(ApiErrorCode.VALIDACION,
                "Ya existe un tipo de actividad con ese nombre en esta categoría.",
                Map.of("nombre", "Ya existe un tipo de actividad con ese nombre en esta categoría."));
    }
}
