package com.activehub.shared.payments;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request payload for creating an order in Mercado Pago API.
 */
public record MercadoPagoOrderRequest(
        String type,
        @JsonProperty("processing_mode") String processingMode,
        @JsonProperty("total_amount") String totalAmount,
        @JsonProperty("external_reference") String externalReference,
        Payer payer,
        Transactions transactions
) {

    public record Payer(
            String email
    ) {
    }

    public record Transactions(
            List<Payment> payments
    ) {
    }

    public record Payment(
            String amount,
            @JsonProperty("payment_method") PaymentMethod paymentMethod
    ) {
    }

    public record PaymentMethod(
            String id,
            String type,
            String token,
            int installments
    ) {
    }
}
