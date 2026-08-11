package com.activehub.shared.error;

import java.util.Map;

public class EmailEnUsoException extends ApiException {

    public EmailEnUsoException() {
        super(ApiErrorCode.EMAIL_EN_USO, "Ese correo ya está registrado.",
                Map.of("email", "Ese correo ya está registrado."));
    }
}
