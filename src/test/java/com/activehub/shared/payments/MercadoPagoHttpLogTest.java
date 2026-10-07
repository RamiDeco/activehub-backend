package com.activehub.shared.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** El log de depuracion nunca puede dejar un secreto completo. */
class MercadoPagoHttpLogTest {

    @Test
    void ocultaElTokenDeLaTarjetaYDejaElResto() {
        String body = "{\"payer\":{\"email\":\"test_user_1@testuser.com\"},\"payment_method\":{\"id\":\"master\","
                + "\"token\":\"0f1e2d3c4b5a69788796a5b4c3d2e1f0\",\"installments\":1}}";

        String log = MercadoPagoHttpLog.ocultar(body, false);

        assertThat(log).doesNotContain("0f1e2d3c4b5a69788796a5b4c3d2e1f0")
                .contains("\"token\":\"0f1e2d3c…(32 caracteres)\"")
                .contains("test_user_1@testuser.com", "\"id\":\"master\"");
    }

    @Test
    void enElCanjeDeOAuthOcultaSecretYCode() {
        String body = "{\"client_id\":\"123\",\"client_secret\":\"s3cr3t0-muy-largo\",\"code\":\"TG-abcdef123456\"}";

        String log = MercadoPagoHttpLog.ocultar(body, true);

        assertThat(log).doesNotContain("s3cr3t0-muy-largo", "TG-abcdef123456").contains("\"client_id\":\"123\"");
    }

    @Test
    void laRespuestaDeOAuthNoDejaLosTokens() {
        String body = "{\"access_token\":\"APP_USR-6119726036486980-100519-22464eea-3670354291\","
                + "\"refresh_token\":\"TG-6119726036486980-xyz\",\"user_id\":3670354291}";

        String log = MercadoPagoHttpLog.ocultar(body, false);

        assertThat(log).doesNotContain("22464eea", "TG-6119726036486980-xyz")
                .contains("APP_USR-…").contains("3670354291");
    }

    @Test
    void fueraDeOAuthElCodeDeErrorSeVe() {
        // En un error de MP "code" es la explicacion ("invalid_credentials"), no un secreto.
        assertThat(MercadoPagoHttpLog.ocultar("{\"errors\":[{\"code\":\"invalid_credentials\"}]}", false))
                .contains("invalid_credentials");
    }

    @Test
    void recortarDejaSoloElPrefijo() {
        assertThat(MercadoPagoHttpLog.recortar("APP_USR-1234567890")).startsWith("APP_USR-").doesNotContain("1234567890");
        assertThat(MercadoPagoHttpLog.recortar("corto")).isEqualTo("***");
    }
}
