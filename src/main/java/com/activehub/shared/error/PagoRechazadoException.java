package com.activehub.shared.error;

import java.util.Map;

/**
 * Mercado Pago proceso el pago y lo rechazo (fondos, datos de la tarjeta, riesgo...). Es 422 y
 * no 502 como {@link PagoException}: no hay nada caido, el alumno puede probar con otra tarjeta.
 * El mensaje traduce el {@code status_detail} de MP; el codigo crudo queda en {@link #getStatusDetail()}.
 */
public class PagoRechazadoException extends ApiException {

    private static final Map<String, String> MOTIVOS = Map.ofEntries(
            Map.entry("insufficient_amount", "La tarjeta no tiene fondos suficientes."),
            Map.entry("cc_rejected_insufficient_amount", "La tarjeta no tiene fondos suficientes."),
            Map.entry("bad_filled_card_data", "Revisá los datos de la tarjeta."),
            Map.entry("cc_rejected_bad_filled_other", "Revisá los datos de la tarjeta."),
            Map.entry("bad_filled_security_code", "Revisá el código de seguridad de la tarjeta."),
            Map.entry("cc_rejected_bad_filled_security_code", "Revisá el código de seguridad de la tarjeta."),
            Map.entry("bad_filled_date", "Revisá la fecha de vencimiento de la tarjeta."),
            Map.entry("cc_rejected_bad_filled_date", "Revisá la fecha de vencimiento de la tarjeta."),
            Map.entry("required_call_for_authorize", "Tenés que autorizar el pago con el banco emisor de tu tarjeta."),
            Map.entry("cc_rejected_call_for_authorize", "Tenés que autorizar el pago con el banco emisor de tu tarjeta."),
            Map.entry("card_disabled", "La tarjeta no está habilitada. Comunicate con tu banco."),
            Map.entry("cc_rejected_card_disabled", "La tarjeta no está habilitada. Comunicate con tu banco."),
            Map.entry("high_risk", "Mercado Pago rechazó el pago. Probá con otra tarjeta."),
            // Orders API: el banco emisor no autorizo (es lo que devuelve el sandbox con titular OTHE).
            Map.entry("rejected_by_issuer", "Tu banco rechazó el pago. Probá con otra tarjeta."),
            Map.entry("cc_rejected_other_reason", "Tu banco rechazó el pago. Probá con otra tarjeta."),
            Map.entry("cc_rejected_high_risk", "Mercado Pago rechazó el pago. Probá con otra tarjeta."),
            Map.entry("max_attempts_exceeded", "Superaste el límite de intentos. Probá con otra tarjeta."),
            Map.entry("cc_rejected_max_attempts", "Superaste el límite de intentos. Probá con otra tarjeta."),
            Map.entry("cc_rejected_duplicated_payment", "Ya hiciste un pago por este mismo monto."));

    private final String statusDetail;

    public PagoRechazadoException(String statusDetail) {
        super(ApiErrorCode.PAGO_RECHAZADO, MOTIVOS.getOrDefault(
                statusDetail, "Mercado Pago rechazó el pago. Probá con otra tarjeta o medio de pago."));
        this.statusDetail = statusDetail;
    }

    public String getStatusDetail() {
        return statusDetail;
    }
}
