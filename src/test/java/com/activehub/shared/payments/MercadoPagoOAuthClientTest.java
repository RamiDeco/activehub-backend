package com.activehub.shared.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.activehub.shared.error.PagoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class MercadoPagoOAuthClientTest {

    private static final String BASE = "https://api.mercadopago.com";

    private MockRestServiceServer server;
    private MercadoPagoProperties props;
    private MercadoPagoOAuthClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        props = new MercadoPagoProperties();
        props.setClientId("123456");
        props.setClientSecret("secreto");
        props.setRedirectUri("https://abc.ngrok.app/api/instructor/mercadopago/callback");
        client = new MercadoPagoOAuthClient(builder.build(), props);
    }

    @Test
    void urlDeAutorizacion_llevaClientIdStateYRedirectUri() {
        var uri = UriComponentsBuilder.fromUriString(client.urlDeAutorizacion("st.at+e/=")).build();

        assertThat(uri.getHost()).isEqualTo("auth.mercadopago.com");
        assertThat(uri.getPath()).isEqualTo("/authorization");
        assertThat(uri.getQueryParams().getFirst("client_id")).isEqualTo("123456");
        assertThat(uri.getQueryParams().getFirst("response_type")).isEqualTo("code");
        assertThat(uri.getQueryParams().getFirst("platform_id")).isEqualTo("mp");
        // Codificado: un "+" o "/" del state no puede llegar alterado al callback.
        assertThat(decodificar(uri.getQueryParams().getFirst("state"))).isEqualTo("st.at+e/=");
        assertThat(uri.getQueryParams().getFirst("state")).doesNotContain("+");
        assertThat(decodificar(uri.getQueryParams().getFirst("redirect_uri"))).isEqualTo(props.getRedirectUri());
        assertThat(client.urlDeAutorizacion("x")).doesNotContain("secreto");
    }

    private static String decodificar(String s) {
        return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    void canjearCodigo_ok_devuelveCredenciales() {
        server.expect(requestTo(BASE + "/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.grant_type").value("authorization_code"))
                .andExpect(jsonPath("$.code").value("TG-123"))
                .andExpect(jsonPath("$.client_id").value("123456"))
                .andExpect(jsonPath("$.client_secret").value("secreto"))
                .andExpect(jsonPath("$.redirect_uri").value(props.getRedirectUri()))
                // Nunca se piden credenciales TEST-: la Orders API las rechaza (401).
                .andExpect(jsonPath("$.test_token").doesNotExist())
                .andRespond(withSuccess("""
                        {"access_token":"APP_USR-at","token_type":"Bearer","expires_in":15552000,
                         "scope":"offline_access payments write","user_id":987654,
                         "refresh_token":"TG-rt","public_key":"APP_USR-pk","live_mode":false}
                        """, MediaType.APPLICATION_JSON));

        var c = client.canjearCodigo("TG-123");

        assertThat(c.accessToken()).isEqualTo("APP_USR-at");
        assertThat(c.refreshToken()).isEqualTo("TG-rt");
        assertThat(c.publicKey()).isEqualTo("APP_USR-pk");
        assertThat(c.userId()).isEqualTo(987654L);
        assertThat(c.expiresIn()).isEqualTo(15552000L);
        assertThat(c.toString()).doesNotContain("APP_USR", "TG-rt");
        server.verify();
    }

    @Test
    void canjearCodigo_rechazadoPorMP_lanzaPagoException() {
        server.expect(requestTo(BASE + "/oauth/token"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\",\"message\":\"Invalid authorization code\"}"));

        assertThatThrownBy(() -> client.canjearCodigo("TG-viejo")).isInstanceOf(PagoException.class);
    }

    @Test
    void renovar_ok_mandaElRefreshTokenYDevuelveElParNuevo() {
        server.expect(requestTo(BASE + "/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.grant_type").value("refresh_token"))
                .andExpect(jsonPath("$.refresh_token").value("TG-rt-viejo"))
                .andExpect(jsonPath("$.client_id").value("123456"))
                .andExpect(jsonPath("$.client_secret").value("secreto"))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andRespond(withSuccess("""
                        {"access_token":"APP_USR-at2","token_type":"Bearer","expires_in":15552000,
                         "user_id":987654,"refresh_token":"TG-rt2","public_key":"APP_USR-pk"}
                        """, MediaType.APPLICATION_JSON));

        var c = client.renovar("TG-rt-viejo");

        assertThat(c.accessToken()).isEqualTo("APP_USR-at2");
        assertThat(c.refreshToken()).isEqualTo("TG-rt2");
        assertThat(c.expiresIn()).isEqualTo(15552000L);
        server.verify();
    }

    @Test
    void renovar_rechazadoPorMP_lanzaRenovacionRechazada() {
        server.expect(requestTo(BASE + "/oauth/token"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\",\"message\":\"invalid refresh_token\"}"));

        assertThatThrownBy(() -> client.renovar("TG-revocado"))
                .isInstanceOf(MercadoPagoOAuthClient.RenovacionRechazadaException.class);
    }

    @Test
    void renovar_errorDelServidorDeMP_esTransitorioYNoRechazo() {
        server.expect(requestTo(BASE + "/oauth/token"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.renovar("TG-rt"))
                .isInstanceOf(PagoException.class)
                .isNotInstanceOf(MercadoPagoOAuthClient.RenovacionRechazadaException.class);
    }

    @Test
    void sinConfiguracion_noLlamaAMercadoPago() {
        props.setClientSecret("");

        assertThatThrownBy(() -> client.canjearCodigo("TG-123")).isInstanceOf(PagoException.class);
        assertThatThrownBy(() -> client.urlDeAutorizacion("s")).isInstanceOf(PagoException.class);
        server.verify();
    }
}
