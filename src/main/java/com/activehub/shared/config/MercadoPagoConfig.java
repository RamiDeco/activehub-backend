package com.activehub.shared.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.activehub.shared.payments.MercadoPagoHttpLog;
import com.activehub.shared.payments.MercadoPagoProperties;
import org.slf4j.LoggerFactory;

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

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Content-Type", "application/json");
        if (LoggerFactory.getLogger(MercadoPagoHttpLog.class).isDebugEnabled()) {
            // MP_LOG_LEVEL=DEBUG: se loguea cada pedido y respuesta (con los secretos recortados).
            // La fabrica "buffering" permite leer el body de la respuesta para el log sin
            // consumirlo; solo se usa con el log activo porque guarda cada respuesta en memoria.
            builder.requestFactory(new BufferingClientHttpRequestFactory(requestFactory))
                    .requestInterceptor(new MercadoPagoHttpLog());
        } else {
            builder.requestFactory(requestFactory);
        }
        return builder.build();
    }
}
