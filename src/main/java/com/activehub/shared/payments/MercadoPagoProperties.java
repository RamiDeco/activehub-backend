package com.activehub.shared.payments;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("app.mercadopago")
public class MercadoPagoProperties {
    private String baseUrl = "https://api.mercadopago.com";
    private String publicKey;
    /** Token de la PLATAFORMA. Las ordenes del marketplace usan el del instructor, no este. */
    private String accessToken;
}
