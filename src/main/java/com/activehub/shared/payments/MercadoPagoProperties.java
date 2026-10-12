package com.activehub.shared.payments;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("app.mercadopago")
public class MercadoPagoProperties {
    private String baseUrl = "https://api.mercadopago.com";
    /** Pantalla de consentimiento de OAuth (dominio distinto al de la API). */
    private String authUrl = "https://auth.mercadopago.com/authorization";
    private String publicKey;
    /** Token de la PLATAFORMA. Las ordenes del marketplace usan el del instructor, no este. */
    private String accessToken;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    /** Solo sandbox: correo del COMPRADOR de prueba, que reemplaza al del alumno como pagador. */
    private String sandboxPayerEmail;

    public boolean oauthConfigurado() {
        return noVacio(clientId) && noVacio(clientSecret) && noVacio(redirectUri);
    }

    private static boolean noVacio(String s) {
        return s != null && !s.isBlank();
    }
}
