package com.activehub.shared.error;

public class ClaseConInscriptosException extends ApiException {

    public ClaseConInscriptosException() {
        super(
                ApiErrorCode.CLASE_CON_INSCRIPTOS,
                "No podés eliminar esta clase porque tiene alumnos anotados. Cancelala en su lugar: "
                        + "así se les avisa y se procesan los reintegros.");
    }
}
