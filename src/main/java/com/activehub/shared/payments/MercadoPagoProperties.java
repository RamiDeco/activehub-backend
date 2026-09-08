package com.activehub.shared.payments;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
@ConfigurationProperties("app.mercadopago")
public class MercadoPagoProperties {
    private String publicKey;
    private String accessToken;
}
