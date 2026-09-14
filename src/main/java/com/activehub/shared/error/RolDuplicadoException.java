package com.activehub.shared.error;

import java.util.Map;

public class RolDuplicadoException extends ApiException {

    public RolDuplicadoException() {
        super(ApiErrorCode.VALIDACION,
                "Ya existe un rol con ese nombre.",
                Map.of("nombre", "Ya existe un rol con ese nombre."));
    }
}
