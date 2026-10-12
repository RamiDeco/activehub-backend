package com.activehub.shared.payments;

import com.activehub.shared.error.PagoException;
import com.activehub.shared.error.PagoRechazadoException;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link PaymentGateway} contra la Orders API de Mercado Pago, con {@code RestClient} propio
 * (MP-01). No hay mock: esta es la unica implementacion, tambien en desarrollo (contra las
 * credenciales TEST de sandbox).
 *
 * <p>Errores: "MP dijo que no" ({@link PagoRechazadoException}, 422, el alumno puede probar
 * con otra tarjeta) es distinto de "MP no respondio bien" ({@link PagoException}, 502).
 */
@Service
public class MercadoPagoGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoGateway.class);

    private final RestClient mpRestClient;
    private final ObjectMapper objectMapper;
    private final MercadoPagoProperties properties;

    public MercadoPagoGateway(RestClient mercadoPagoRestClient, ObjectMapper objectMapper,
                              MercadoPagoProperties properties) {
        this.mpRestClient = mercadoPagoRestClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public ResultadoPago autorizar(String sellerToken, DatosTarjeta tarjeta, BigDecimal monto, BigDecimal comision,
                                   String externalReference, String idempotencyKey) {
        String total = monto.setScale(2).toPlainString();
        var body = new MercadoPagoOrderRequest(
                "online", "automatic", "manual", total, externalReference,
                comision == null || comision.signum() == 0 ? null : comision.setScale(2).toPlainString(),
                new MercadoPagoOrderRequest.Payer(emailPagador(tarjeta)),
                new MercadoPagoOrderRequest.Transactions(List.of(new MercadoPagoOrderRequest.Payment(
                        total,
                        new MercadoPagoOrderRequest.PaymentMethod(
                                tarjeta.paymentMethodId(), tarjeta.paymentMethodType(), tarjeta.token(),
                                tarjeta.cuotas())))));
        return llamar("autorizar", externalReference, () -> mpRestClient.post()
                .uri("/v1/orders")
                .header("Authorization", bearer(sellerToken))
                .header("X-Idempotency-Key", idempotencyKey)
                .body(body)
                .retrieve()
                .body(MercadoPagoOrderResponse.class));
    }

    @Override
    public ResultadoPago capturar(String sellerToken, String orderId, String idempotencyKey) {
        return accion("capture", sellerToken, orderId, idempotencyKey);
    }

    @Override
    public ResultadoPago cancelarAutorizacion(String sellerToken, String orderId, String idempotencyKey) {
        return accion("cancel", sellerToken, orderId, idempotencyKey);
    }

    @Override
    public ResultadoPago reembolsar(String sellerToken, String orderId, String idempotencyKey) {
        return accion("refund", sellerToken, orderId, idempotencyKey);
    }

    @Override
    public ResultadoPago consultar(String sellerToken, String orderId) {
        return llamar("consultar", orderId, () -> mpRestClient.get()
                .uri("/v1/orders/{id}", orderId)
                .header("Authorization", bearer(sellerToken))
                .retrieve()
                .body(MercadoPagoOrderResponse.class));
    }

    private ResultadoPago accion(String accion, String sellerToken, String orderId, String idempotencyKey) {
        return llamar(accion, orderId, () -> mpRestClient.post()
                .uri("/v1/orders/{id}/" + accion, orderId)
                .header("Authorization", bearer(sellerToken))
                .header("X-Idempotency-Key", idempotencyKey)
                .retrieve()
                .body(MercadoPagoOrderResponse.class));
    }

    private ResultadoPago llamar(String operacion, String referencia, Supplier<MercadoPagoOrderResponse> llamada) {
        MercadoPagoOrderResponse response;
        try {
            response = llamada.get();
        } catch (RestClientResponseException ex) {
            // El body de MP repite el token de la tarjeta: nunca se loguea sin recortar.
            String cuerpo = MercadoPagoHttpLog.ocultar(ex.getResponseBodyAsString(), false);
            MercadoPagoOrderResponse fallida = ordenFallida(ex.getResponseBodyAsString());
            if (fallida != null) {
                // Un rechazo de la tarjeta es un resultado del negocio, no una falla del sistema:
                // WARN y no ERROR, para que el monitoreo no trate cada tarjeta rechazada como alarma.
                String detalle = detalleRechazo(fallida);
                log.warn("Mercado Pago rechazo el pago al {} {}: {} (HTTP {})", operacion, referencia, detalle,
                        ex.getStatusCode().value());
                log.debug("Respuesta completa del rechazo: {}", cuerpo);
                throw new PagoRechazadoException(detalle);
            }
            log.error("Mercado Pago HTTP {} al {} {}: {}", ex.getStatusCode(), operacion, referencia, cuerpo);
            if (ex.getStatusCode().is4xxClientError()) {
                throw new PagoException("Mercado Pago no aceptó la operación (" + ex.getStatusCode().value() + ").");
            }
            throw new PagoException("Mercado Pago no está respondiendo. Intentá nuevamente en unos minutos.");
        } catch (ResourceAccessException ex) {
            log.error("Sin conexion o timeout con Mercado Pago al {} {}: {}", operacion, referencia, ex.getMessage());
            throw new PagoException("No se pudo conectar con Mercado Pago. Intentá nuevamente.");
        }

        if (response == null || response.id() == null) {
            log.error("Mercado Pago devolvio una respuesta vacia al {} {}", operacion, referencia);
            throw new PagoException("No se pudo obtener una respuesta válida de Mercado Pago.");
        }
        if ("failed".equals(response.status())) {
            String detalle = detalleRechazo(response);
            log.warn("Mercado Pago rechazo el pago al {} {}: {}", operacion, referencia, detalle);
            throw new PagoRechazadoException(detalle);
        }
        return new ResultadoPago(response.id(), response.status(), response.statusDetail());
    }

    /** El rechazo llega como error HTTP con la orden en {@code data} ({@code status: failed}). */
    private MercadoPagoOrderResponse ordenFallida(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            var data = objectMapper.readTree(body).get("data");
            if (data == null || data.isNull()) {
                return null;
            }
            var orden = objectMapper.treeToValue(data, MercadoPagoOrderResponse.class);
            return "failed".equals(orden.status()) ? orden : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** El motivo util esta en la transaccion, no en la orden (que dice solo "failed"). */
    private static String detalleRechazo(MercadoPagoOrderResponse orden) {
        if (orden.transactions() != null && orden.transactions().payments() != null) {
            for (var pago : orden.transactions().payments()) {
                if (pago.statusDetail() != null) {
                    return pago.statusDetail();
                }
            }
        }
        return orden.statusDetail();
    }

    /**
     * En sandbox el pagador es el COMPRADOR de prueba (correo @testuser.com), como en los
     * ejemplos de la documentacion de MP: con {@code MP_SANDBOX_PAYER_EMAIL} cargada se manda
     * ese; en produccion queda vacia y va el del alumno. Es un detalle de como se habla con MP,
     * por eso vive aca y no en inscribirse.
     */
    private String emailPagador(DatosTarjeta tarjeta) {
        String sandbox = properties.getSandboxPayerEmail();
        return sandbox != null && !sandbox.isBlank() ? sandbox : tarjeta.emailPagador();
    }

    private static String bearer(String sellerToken) {
        if (sellerToken == null || sellerToken.isBlank()) {
            throw new PagoException("El instructor no tiene una cuenta de Mercado Pago vinculada.");
        }
        return "Bearer " + sellerToken;
    }
}
