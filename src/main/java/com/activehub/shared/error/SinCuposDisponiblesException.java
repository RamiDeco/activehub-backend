package com.activehub.shared.error;

public class SinCuposDisponiblesException extends ApiException {

    public SinCuposDisponiblesException() {
        super(ApiErrorCode.SIN_CUPOS_DISPONIBLES, "No quedan cupos disponibles para esta clase.");
    }
}
