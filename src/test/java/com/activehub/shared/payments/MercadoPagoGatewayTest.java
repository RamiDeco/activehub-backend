package com.activehub.shared.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.activehub.shared.error.ApiErrorCode;
import com.activehub.shared.error.PagoException;
import com.activehub.shared.error.PagoRechazadoException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * La pasarela contra respuestas HTTP simuladas de la Orders API. Los cuerpos son los de la
 * documentacion de Mercado Pago (captura diferida con Checkout API via Orders).
 */
class MercadoPagoGatewayTest {

    private static final String BASE = "https://api.mercadopago.com";
    private static final String SELLER = "TEST-seller-token";
    private static final PaymentGateway.DatosTarjeta TARJETA =
            new PaymentGateway.DatosTarjeta("card-tok", "master", "credit_card", 1, "alumno@test.com");

    private MockRestServiceServer server;
    private MercadoPagoGateway gateway;
    private MercadoPagoProperties props;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        props = new MercadoPagoProperties();
        gateway = new MercadoPagoGateway(builder.build(), JsonMapper.builder().build(), props);
    }

    private static String orden(String status, String detail) {
        return """
                {"id":"ORD01ABC","status":"%s","status_detail":"%s","capture_mode":"manual",
                 "transactions":{"payments":[{"id":"PAY01","status":"%s","status_detail":"%s"}]}}
                """.formatted(status, detail, status, detail);
    }

    @Test
    void autorizar_ok_mandaCapturaManualConTokenDelVendedorYLaKeyRecibida() {
        server.expect(requestTo(BASE + "/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + SELLER))
                .andExpect(header("X-Idempotency-Key", "key-fija-123"))
                .andExpect(jsonPath("$.capture_mode").value("manual"))
                .andExpect(jsonPath("$.processing_mode").value("automatic"))
                .andExpect(jsonPath("$.total_amount").value("4500.00"))
                .andExpect(jsonPath("$.external_reference").value("insc-1"))
                .andExpect(jsonPath("$.marketplace_fee").doesNotExist())
                .andExpect(jsonPath("$.payer.email").value("alumno@test.com"))
                .andExpect(jsonPath("$.transactions.payments[0].payment_method.token").value("card-tok"))
                .andRespond(withSuccess(orden("action_required", "waiting_capture"), MediaType.APPLICATION_JSON));

        var r = gateway.autorizar(SELLER, TARJETA, new BigDecimal("4500"), BigDecimal.ZERO, "insc-1", "key-fija-123");

        assertThat(r.orderId()).isEqualTo("ORD01ABC");
        assertThat(r.autorizado()).isTrue();
        server.verify();
    }

    @Test
    void autorizar_enSandbox_mandaComoPagadorAlCompradorDePrueba() {
        // En sandbox el pagador es el comprador de prueba, no el correo real del alumno.
        props.setSandboxPayerEmail("test_user_123@testuser.com");
        server.expect(requestTo(BASE + "/v1/orders"))
                .andExpect(jsonPath("$.payer.email").value("test_user_123@testuser.com"))
                .andRespond(withSuccess(orden("action_required", "waiting_capture"), MediaType.APPLICATION_JSON));

        gateway.autorizar(SELLER, TARJETA, new BigDecimal("4500"), BigDecimal.ZERO, "insc-1", "k");
        server.verify();
    }

    @Test
    void autorizar_conComision_laMandaComoMarketplaceFee() {
        server.expect(requestTo(BASE + "/v1/orders"))
                .andExpect(jsonPath("$.marketplace_fee").value("450.00"))
                .andRespond(withSuccess(orden("action_required", "waiting_capture"), MediaType.APPLICATION_JSON));

        gateway.autorizar(SELLER, TARJETA, new BigDecimal("4500"), new BigDecimal("450"), "insc-1", "k");
        server.verify();
    }

    @Test
    void autorizar_rechazado_lanzaPagoRechazadoConElMotivoDeLaTransaccion() {
        String body = """
                {"errors":[{"code":"failed","message":"The following transactions failed",
                  "details":["PAY01: cc_rejected_insufficient_amount"]}],
                 "data":%s}
                """.formatted(orden("failed", "failed").replace("\"status_detail\":\"failed\",\"capture",
                "\"status_detail\":\"failed\",\"capture"))
                .replace("\"status\":\"failed\",\"status_detail\":\"failed\"}]",
                        "\"status\":\"failed\",\"status_detail\":\"cc_rejected_insufficient_amount\"}]");
        server.expect(requestTo(BASE + "/v1/orders"))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED).contentType(MediaType.APPLICATION_JSON).body(body));

        assertThatThrownBy(() -> gateway.autorizar(SELLER, TARJETA, new BigDecimal("4500"), BigDecimal.ZERO, "i", "k"))
                .isInstanceOfSatisfying(PagoRechazadoException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ApiErrorCode.PAGO_RECHAZADO);
                    assertThat(e.getStatusDetail()).isEqualTo("cc_rejected_insufficient_amount");
                    assertThat(e.getMessage()).isEqualTo("La tarjeta no tiene fondos suficientes.");
                });
    }

    @Test
    void autorizar_failedEnRespuesta200_tambienEsRechazo() {
        server.expect(requestTo(BASE + "/v1/orders"))
                .andRespond(withSuccess(orden("failed", "cc_rejected_high_risk"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gateway.autorizar(SELLER, TARJETA, new BigDecimal("4500"), BigDecimal.ZERO, "i", "k"))
                .isInstanceOf(PagoRechazadoException.class);
    }

    @Test
    void autorizar_error5xx_lanzaPagoExceptionNoRechazo() {
        server.expect(requestTo(BASE + "/v1/orders")).andRespond(withServerError());

        assertThatThrownBy(() -> gateway.autorizar(SELLER, TARJETA, new BigDecimal("4500"), BigDecimal.ZERO, "i", "k"))
                .isExactlyInstanceOf(PagoException.class)
                .satisfies(e -> assertThat(((PagoException) e).getCode()).isEqualTo(ApiErrorCode.ERROR_PAGO));
    }

    @Test
    void autorizar_error4xxSinOrden_lanzaPagoException() {
        server.expect(requestTo(BASE + "/v1/orders"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errors\":[{\"code\":\"unauthorized\"}]}"));

        assertThatThrownBy(() -> gateway.autorizar(SELLER, TARJETA, new BigDecimal("4500"), BigDecimal.ZERO, "i", "k"))
                .isExactlyInstanceOf(PagoException.class);
    }

    @Test
    void autorizar_timeout_lanzaPagoException() {
        server.expect(requestTo(BASE + "/v1/orders"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> gateway.autorizar(SELLER, TARJETA, new BigDecimal("4500"), BigDecimal.ZERO, "i", "k"))
                .isExactlyInstanceOf(PagoException.class)
                .hasMessageContaining("No se pudo conectar");
    }

    @Test
    void sinTokenDelVendedor_noLlamaAMercadoPago() {
        assertThatThrownBy(() -> gateway.autorizar(" ", TARJETA, new BigDecimal("4500"), BigDecimal.ZERO, "i", "k"))
                .isInstanceOf(PagoException.class);
        server.verify();
    }

    @Test
    void capturarCancelarReembolsar_peganAlEndpointDeLaOrdenConSuKey() {
        for (String accion : new String[] {"capture", "cancel", "refund"}) {
            server.expect(requestTo(BASE + "/v1/orders/ORD01ABC/" + accion))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("Authorization", "Bearer " + SELLER))
                    .andExpect(header("X-Idempotency-Key", "k-" + accion))
                    .andRespond(withSuccess(orden("processed", "accredited"), MediaType.APPLICATION_JSON));
        }

        assertThat(gateway.capturar(SELLER, "ORD01ABC", "k-capture").status()).isEqualTo("processed");
        gateway.cancelarAutorizacion(SELLER, "ORD01ABC", "k-cancel");
        gateway.reembolsar(SELLER, "ORD01ABC", "k-refund");
        server.verify();
    }

    @Test
    void consultar_esUnGetSinIdempotencyKey() {
        server.expect(requestTo(BASE + "/v1/orders/ORD01ABC"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> assertThat(request.getHeaders().containsHeader("X-Idempotency-Key")).isFalse())
                .andRespond(withSuccess(orden("action_required", "waiting_capture"), MediaType.APPLICATION_JSON));

        assertThat(gateway.consultar(SELLER, "ORD01ABC").autorizado()).isTrue();
    }
}
