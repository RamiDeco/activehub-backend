package com.activehub.shared.config;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.activehub.shared.payments.MercadoPagoProperties;

@Configuration
@EnableConfigurationProperties(MercadoPagoProperties.class)
public class MercadoPagoConfig {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoConfig.class);

    /**
     * Sin MP_ACCESS_TOKEN la app igual arranca (dev local y tests no dependen de la credencial).
     * No se cae al mock: un pago con Mercado Pago falla en MercadoPagoPaymentGateway con un
     * error explícito, para que una credencial faltante en producción no confirme inscripciones sin cobrar.
     */
    @Bean
    public RestClient mercadoPagoRestClient(MercadoPagoProperties props) {
        String accessToken = props.getAccessToken();
        boolean configurado = accessToken != null && !accessToken.isBlank();

        if (!configurado) {
            log.warn("MP_ACCESS_TOKEN no está configurado: los pagos con Mercado Pago van a fallar.");
        }

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.mercadopago.com")
                .requestFactory(requestFactory)
                .defaultHeader("Content-Type", "application/json");
        if (configurado) {
            builder.defaultHeader("Authorization", "Bearer " + accessToken);
        }
        return builder.build();
    }
}
