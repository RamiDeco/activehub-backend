package com.activehub.shared.error;

/**
 * KAN-55: el instructor quiere desvincular su cuenta de Mercado Pago pero tiene pagos que
 * todavia se cobran, se devuelven o se resuelven con su token. Desvincular borra el token, y
 * sin el esos pagos quedan sin forma de capturarse ni de reintegrarse.
 */
public class CuentaMercadoPagoConPagosPendientesException extends ApiException {

    public CuentaMercadoPagoConPagosPendientesException(long pagosPendientes) {
        super(ApiErrorCode.MERCADOPAGO_CON_PAGOS_PENDIENTES,
                "No podés desvincular tu cuenta de Mercado Pago todavía: "
                        + (pagosPendientes == 1
                                ? "hay 1 pago de un alumno que todavía se cobra o se devuelve a través de ella. "
                                : "hay " + pagosPendientes
                                        + " pagos de alumnos que todavía se cobran o se devuelven a través de ella. ")
                        + "Vas a poder desvincularla cuando esas clases finalicen y los pagos se acrediten.");
    }
}
