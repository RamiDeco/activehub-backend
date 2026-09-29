package com.activehub.shared.payments;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Body de POST /v1/orders. Interno de {@link MercadoPagoGateway}: no es un DTO de la API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoOrderRequest(
        String type,
        @JsonProperty("processing_mode") String processingMode,
        /** "manual": la orden queda autorizada y hay que capturarla (MP-01). */
        @JsonProperty("capture_mode") String captureMode,
        @JsonProperty("total_amount") String totalAmount,
        @JsonProperty("external_reference") String externalReference,
        /** Comision de la plataforma (solo con OAuth). Null = no se envia. */
        @JsonProperty("marketplace_fee") String marketplaceFee,
        Payer payer,
        Transactions transactions
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payer(
            String email
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transactions(
            List<Payment> payments
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payment(
            String amount,
            @JsonProperty("payment_method") PaymentMethod paymentMethod
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentMethod(
            String id,
            String type,
            String token,
            int installments
    ) {
    }
}
