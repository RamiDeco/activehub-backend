package com.activehub.shared.payments;

import com.activehub.shared.error.PagoException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class MercadoPagoPaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoPaymentGateway.class);
    private final RestClient mpRestClient;
    private final MercadoPagoProperties properties;

    /**
     * Creates an order in Mercado Pago and handles HTTP communication errors.
     */
    public MercadoPagoOrderResponse createOrder(MercadoPagoOrderRequest request) {
        String accessToken = properties.getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            log.error("Intento de pago con Mercado Pago sin MP_ACCESS_TOKEN configurado");
            throw new PagoException("El pago con Mercado Pago no está disponible en este momento.");
        }
        String idempotencyKey = UUID.randomUUID().toString();
        try {
            MercadoPagoOrderResponse response = mpRestClient.post()
                    .uri("/v1/orders")
                    .header("X-Idempotency-Key", idempotencyKey)
                    .body(request)
                    .retrieve()
                    .body(MercadoPagoOrderResponse.class);

            if (response == null || response.id() == null) {
                log.error("Mercado Pago returned an empty or invalid response for order: {}", request.externalReference());
                throw new PagoException("No se pudo obtener una respuesta válida del procesador de pagos.");
            }

            return response;
        } catch (RestClientResponseException ex) {
            // Logs 4xx and 5xx responses with the exact response body returned by MP
            log.error("Mercado Pago HTTP error [status: {}]: {}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new PagoException("Error al procesar el pago con Mercado Pago: " + ex.getStatusCode());
        } catch (ResourceAccessException ex) {
            // Handles network issues, DNS failures or read/connection timeouts
            log.error("Mercado Pago connection or timeout error: {}", ex.getMessage(), ex);
            throw new PagoException("No se pudo conectar con el procesador de pagos. Intentá nuevamente.");
        } catch (PagoException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while calling Mercado Pago: {}", ex.getMessage(), ex);
            throw new PagoException("Ocurrió un error inesperado al procesar el pago.");
        }
    }
}

