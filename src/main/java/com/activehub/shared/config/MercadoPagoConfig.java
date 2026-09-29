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

    /**
     * Cliente HTTP de la Orders API. <b>Sin {@code Authorization} por defecto</b>: en el
     * marketplace cada llamada va con el token del instructor dueno de la orden, y un default
     * con el token de la plataforma haria que un olvido cobre en la cuenta equivocada.
     * {@link com.activehub.shared.payments.MercadoPagoGateway} lo pone en cada request.
     *
     * <p>Timeouts cortos a proposito: la autorizacion corre dentro de la transaccion de
     * inscribirse, con un cupo tomado.
     */
    @Bean
    public RestClient mercadoPagoRestClient(MercadoPagoProperties props) {
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
