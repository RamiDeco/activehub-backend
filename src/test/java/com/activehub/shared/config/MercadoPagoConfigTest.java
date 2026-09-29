package com.activehub.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.activehub.shared.error.PagoException;
import com.activehub.shared.payments.MercadoPagoOrderRequest;
import com.activehub.shared.payments.MercadoPagoPaymentGateway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Sin MP_ACCESS_TOKEN la app tiene que arrancar (dev local y tests no dependen de la credencial),
 * pero un pago con Mercado Pago tiene que fallar explícito, sin llamar a la API ni caer al mock.
 */
class MercadoPagoConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MercadoPagoConfig.class, MercadoPagoPaymentGateway.class);

    @Test
    void sinToken_elContextoArranca() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MercadoPagoPaymentGateway.class);
        });
    }

    @Test
    void sinToken_pagarLanzaPagoException() {
        runner.run(context -> {
            var gateway = context.getBean(MercadoPagoPaymentGateway.class);
            var request = new MercadoPagoOrderRequest("online", "automatic", "100", "ref", null, null);

            assertThatThrownBy(() -> gateway.createOrder(request))
                    .isInstanceOf(PagoException.class)
                    .hasMessageContaining("no está disponible");
        });
    }

    @Test
    void tokenEnBlanco_seTrataComoFaltante() {
        runner.withPropertyValues("app.mercadopago.access-token=   ").run(context -> {
            assertThat(context).hasNotFailed();
            var gateway = context.getBean(MercadoPagoPaymentGateway.class);
            var request = new MercadoPagoOrderRequest("online", "automatic", "100", "ref", null, null);

            assertThatThrownBy(() -> gateway.createOrder(request)).isInstanceOf(PagoException.class);
        });
    }
}
