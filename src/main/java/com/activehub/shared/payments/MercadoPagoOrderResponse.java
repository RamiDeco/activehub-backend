package com.activehub.shared.payments;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response payload returned when creating or querying an order in Mercado Pago API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoOrderResponse(
        String id,
        String type,
        @JsonProperty("processing_mode") String processingMode,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("total_amount") String totalAmount,
        @JsonProperty("total_paid_amount") String totalPaidAmount,
        @JsonProperty("country_code") String countryCode,
        @JsonProperty("user_id") String userId,
        String status,
        @JsonProperty("status_detail") String statusDetail,
        @JsonProperty("capture_mode") String captureMode,
        @JsonProperty("created_date") String createdDate,
        @JsonProperty("last_updated_date") String lastUpdatedDate,
        @JsonProperty("integration_data") IntegrationData integrationData,
        Transactions transactions
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IntegrationData(
            @JsonProperty("application_id") String applicationId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transactions(
            List<Payment> payments
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payment(
            String id,
            String amount,
            @JsonProperty("paid_amount") String paidAmount,
            @JsonProperty("reference_id") String referenceId,
            String status,
            @JsonProperty("status_detail") String statusDetail,
            @JsonProperty("payment_method") PaymentMethod paymentMethod
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentMethod(
            String id,
            String type,
            String token,
            // Integer y no int: Jackson 3 falla si a un primitivo le falta el valor, y entonces un
            // rechazo de MP sin este campo se perdia como error generico (ordenFallida -> null).
            Integer installments
    ) {
    }
}
