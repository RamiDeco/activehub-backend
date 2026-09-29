package com.activehub.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.activehub.shared.payments.MercadoPagoGateway;
import com.activehub.shared.payments.PaymentGateway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sin credenciales la app arranca igual (dev local y tests no dependen de ellas), y la unica
 * pasarela es la real: no hay mock al que caer (decision de MP-04).
 */
class MercadoPagoConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, () -> JsonMapper.builder().build())
            .withUserConfiguration(MercadoPagoConfig.class, MercadoPagoGateway.class);

    @Test
    void sinCredenciales_elContextoArrancaConLaPasarelaReal() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(PaymentGateway.class);
            assertThat(context.getBean(PaymentGateway.class)).isInstanceOf(MercadoPagoGateway.class);
        });
    }
}
