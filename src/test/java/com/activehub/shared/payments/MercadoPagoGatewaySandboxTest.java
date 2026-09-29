package com.activehub.shared.payments;

import static org.assertj.core.api.Assertions.assertThat;

import com.activehub.shared.config.MercadoPagoConfig;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Prueba MANUAL contra el sandbox real de Mercado Pago (criterios de KAN-53). No corre en la
 * suite: sacar el {@code @Disabled}, cargar las variables y correr
 * {@code ./mvnw test -Dtest=MercadoPagoGatewaySandboxTest}.
 *
 * <ul>
 *   <li>{@code MP_SANDBOX_SELLER_ACCESS_TOKEN}: access token TEST del usuario de prueba VENDEDOR.</li>
 *   <li>{@code MP_SANDBOX_SELLER_PUBLIC_KEY}: su public key TEST (para tokenizar la tarjeta).</li>
 *   <li>{@code MP_SANDBOX_BUYER_EMAIL}: correo del usuario de prueba COMPRADOR.</li>
 * </ul>
 *
 * <p>Despues de cada test, buscar la orden impresa en el panel del vendedor de prueba y
 * verificar el estado: autorizada → aprobada, y autorizada → cancelada.
 */
@Disabled("Manual: pega contra el sandbox de Mercado Pago con credenciales TEST")
class MercadoPagoGatewaySandboxTest {

    private final String sellerToken = System.getenv("MP_SANDBOX_SELLER_ACCESS_TOKEN");
    private final String sellerPublicKey = System.getenv("MP_SANDBOX_SELLER_PUBLIC_KEY");
    private final String buyerEmail = System.getenv("MP_SANDBOX_BUYER_EMAIL");

    private final RestClient restClient = new MercadoPagoConfig().mercadoPagoRestClient(new MercadoPagoProperties());
    private final MercadoPagoGateway gateway = new MercadoPagoGateway(restClient, JsonMapper.builder().build());

    @Test
    void autorizarYCapturar() {
        var autorizada = autorizar();
        System.out.println("Orden autorizada: " + autorizada);
        assertThat(autorizada.autorizado()).isTrue();

        var capturada = gateway.capturar(sellerToken, autorizada.orderId(), UUID.randomUUID().toString());
        System.out.println("Orden capturada: " + capturada);
        assertThat(capturada.status()).isEqualTo("processed");
    }

    @Test
    void autorizarYCancelar() {
        var autorizada = autorizar();
        System.out.println("Orden autorizada: " + autorizada);
        assertThat(autorizada.autorizado()).isTrue();

        var cancelada = gateway.cancelarAutorizacion(sellerToken, autorizada.orderId(), UUID.randomUUID().toString());
        System.out.println("Orden cancelada: " + cancelada);
        assertThat(cancelada.status()).isEqualTo("canceled");
    }

    private PaymentGateway.ResultadoPago autorizar() {
        var tarjeta = new PaymentGateway.DatosTarjeta(
                tokenizarTarjetaDePrueba(), "master", "credit_card", 1, buyerEmail);
        return gateway.autorizar(sellerToken, tarjeta, new BigDecimal("100"), BigDecimal.ZERO,
                "sandbox-" + UUID.randomUUID(), UUID.randomUUID().toString());
    }

    /** Mastercard de prueba de Argentina; titular "APRO" = el sandbox la aprueba. */
    @SuppressWarnings("unchecked")
    private String tokenizarTarjetaDePrueba() {
        Map<String, Object> respuesta = restClient.post()
                .uri("/v1/card_tokens?public_key={pk}", sellerPublicKey)
                .body(Map.of(
                        "card_number", "5031755734530604",
                        "expiration_month", 11,
                        "expiration_year", 2030,
                        "security_code", "123",
                        "cardholder", Map.of(
                                "name", "APRO",
                                "identification", Map.of("type", "DNI", "number", "12345678"))))
                .retrieve()
                .body(Map.class);
        return (String) respuesta.get("id");
    }
}
