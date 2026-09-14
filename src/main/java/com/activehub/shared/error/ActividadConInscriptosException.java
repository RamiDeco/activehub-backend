package com.activehub.shared.error;

public class ActividadConInscriptosException extends ApiException {

    public ActividadConInscriptosException() {
        super(
                ApiErrorCode.ACTIVIDAD_CON_INSCRIPTOS,
                "No podés eliminar esta actividad porque tiene clases con inscriptos o pagos pendientes. "
                        + "Primero cancelá las clases correspondientes.");
    }
}
