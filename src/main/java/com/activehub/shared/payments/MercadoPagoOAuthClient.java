package com.activehub.shared.payments;

import com.activehub.shared.error.PagoException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Lado servidor del OAuth de Mercado Pago (marketplace): arma la URL de consentimiento y
 * canjea el {@code code} en {@code POST /oauth/token}. El {@code client_secret} nunca sale
 * del backend.
 */
@Component
public class MercadoPagoOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoOAuthClient.class);

    private final RestClient mpRestClient;
    private final MercadoPagoProperties properties;

    public MercadoPagoOAuthClient(RestClient mercadoPagoRestClient, MercadoPagoProperties properties) {
        this.mpRestClient = mercadoPagoRestClient;
        this.properties = properties;
    }

    public String urlDeAutorizacion(String state) {
        exigirConfiguracion();
        // Como variables y no literales: asi encode() las codifica en forma estricta ("+", "/",
        // "=" incluidos) y el state vuelve al callback exactamente igual.
        return org.springframework.web.util.UriComponentsBuilder.fromUriString(properties.getAuthUrl())
                .queryParam("client_id", "{clientId}")
                .queryParam("response_type", "code")
                .queryParam("platform_id", "mp")
                .queryParam("state", "{state}")
                .queryParam("redirect_uri", "{redirectUri}")
                .encode()
                .buildAndExpand(properties.getClientId(), state, properties.getRedirectUri())
                .toUriString();
    }

    /**
     * @throws PagoException si MP rechaza el codigo (vencido, ya usado, de otra app) o no responde
     */
    public CredencialesOAuth canjearCodigo(String code) {
        exigirConfiguracion();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("client_id", properties.getClientId());
        body.put("client_secret", properties.getClientSecret());
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        body.put("redirect_uri", properties.getRedirectUri());
        // Sin "test_token": pide credenciales TEST-, que la Orders API rechaza. En sandbox la
        // prueba la dan las cuentas (vendedor de prueba), no el tipo de credencial.

        CredencialesOAuth credenciales;
        try {
            credenciales = mpRestClient.post()
                    .uri("/oauth/token")
                    .body(body)
                    .retrieve()
                    .body(CredencialesOAuth.class);
        } catch (RestClientResponseException ex) {
            // El body de error de MP no trae secretos (solo codigo y mensaje), se puede loguear.
            log.warn("Mercado Pago rechazo el canje del codigo OAuth [{}]: {}", ex.getStatusCode(),
                    ex.getResponseBodyAsString());
            throw new PagoException("Mercado Pago no aceptó la vinculación.");
        } catch (ResourceAccessException ex) {
            log.error("Sin conexion o timeout con Mercado Pago al canjear el codigo OAuth: {}", ex.getMessage());
            throw new PagoException("No se pudo conectar con Mercado Pago. Intentá nuevamente.");
        }
        if (credenciales == null || credenciales.accessToken() == null || credenciales.userId() == null) {
            throw new PagoException("Mercado Pago devolvió una respuesta incompleta al vincular la cuenta.");
        }
        return credenciales;
    }

    private void exigirConfiguracion() {
        if (!properties.oauthConfigurado()) {
            log.error("OAuth de Mercado Pago sin configurar: faltan MP_CLIENT_ID, MP_CLIENT_SECRET o MP_REDIRECT_URI");
            throw new PagoException("La vinculación con Mercado Pago no está disponible en este momento.");
        }
    }

    /** Respuesta de /oauth/token. Contiene secretos: no loguear ni devolver por la API. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CredencialesOAuth(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("public_key") String publicKey,
            @JsonProperty("user_id") Long userId,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("live_mode") Boolean liveMode
    ) {
        @Override
        public String toString() {
            return "CredencialesOAuth[userId=" + userId + ", liveMode=" + liveMode + "]";
        }
    }
}
