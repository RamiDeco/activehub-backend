package com.activehub.shared.ia;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/**
 * De qué se entera el usuario cuando el proveedor dice "no": el tiempo de espera.
 *
 * <p>No se prueba la llamada HTTP en sí — eso necesitaría un servidor de mentira y una clave — sino
 * la lectura del 429, que es la parte con reglas nuestras: el header manda, el cuerpo es el respaldo
 * y, sin ninguno de los dos, se promete un minuto en vez de no prometer nada.
 */
class ModeloLenguajeGroqTest {

    private HttpClientErrorException error429(HttpHeaders headers, String cuerpo) {
        return new HttpClientErrorException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                headers == null ? HttpHeaders.EMPTY : headers,
                cuerpo == null ? new byte[0] : cuerpo.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
    }

    @Test
    void esperaDe_usaElHeaderRetryAfter() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("retry-after", "42");

        assertThat(ModeloLenguajeGroq.esperaDe(error429(headers, null)))
                .isEqualTo(Duration.ofSeconds(42));
    }

    @Test
    void esperaDe_redondeaHaciaArribaLosDecimalesDelHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("retry-after", "7.2");

        // Decirle "esperá 7" cuando faltan 7,2 es mandarla a chocarse con el mismo cartel.
        assertThat(ModeloLenguajeGroq.esperaDe(error429(headers, null)))
                .isEqualTo(Duration.ofSeconds(8));
    }

    @Test
    void esperaDe_sinHeader_loLeeDelMensajeDelProveedor() {
        String cuerpo = """
                {"error":{"message":"Rate limit reached for model `llama-3.3-70b-versatile`. \
                Please try again in 2m59.56s.","type":"tokens"}}""";

        assertThat(ModeloLenguajeGroq.esperaDe(error429(null, cuerpo)))
                .isEqualTo(Duration.ofSeconds(180));
    }

    @Test
    void esperaDe_sinHeaderNiMensaje_asumeUnMinuto() {
        assertThat(ModeloLenguajeGroq.esperaDe(error429(null, "{}")))
                .isEqualTo(Duration.ofMinutes(1));
    }
}
