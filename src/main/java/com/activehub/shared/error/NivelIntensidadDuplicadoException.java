package com.activehub.shared.error;

import java.util.Map;

public class NivelIntensidadDuplicadoException extends ApiException {

    public NivelIntensidadDuplicadoException() {
        super(ApiErrorCode.VALIDACION,
                "Ya existe un nivel de intensidad con ese nombre.",
                Map.of("nombre", "Ya existe un nivel de intensidad con ese nombre."));
    }
}
