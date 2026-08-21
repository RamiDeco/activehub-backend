package com.activehub.shared.error;

import java.util.Map;

public class CategoriaDuplicadaException extends ApiException {

    public CategoriaDuplicadaException() {
        super(ApiErrorCode.VALIDACION,
                "Ya existe una categoría con ese nombre.",
                Map.of("nombre", "Ya existe una categoría con ese nombre."));
    }
}
