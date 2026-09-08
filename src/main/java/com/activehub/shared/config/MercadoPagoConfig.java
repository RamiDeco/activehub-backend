package com.activehub.shared.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.activehub.shared.payments.MercadoPagoProperties;

@Configuration
@EnableConfigurationProperties(MercadoPagoProperties.class)
public class MercadoPagoConfig {

    @Bean 
    public RestClient mercadoPagoRestClient(MercadoPagoProperties props) {
        String accessToken = props.getAccessToken();

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("Mercado Pago access token is missing or not configured");
        }

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .baseUrl("https://api.mercadopago.com")
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
