package com.activehub.shared.error;

import java.util.Map;

/**
 * Precondicion de E1A-HU03 y E1A-HU04: "no debe existir una cuenta activa con el mismo correo
 * electronico (o DNI)". El DNI es la otra clave de unicidad de la cuenta, no un dato mas.
 */
public class DniEnUsoException extends ApiException {

    public DniEnUsoException() {
        super(ApiErrorCode.DNI_EN_USO, "Ese DNI ya está registrado.",
                Map.of("dni", "Ese DNI ya está registrado."));
    }
}
