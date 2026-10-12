package com.activehub.shared.payments;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

/**
 * Loguea cada pedido a Mercado Pago tal como sale (metodo, URL, headers, body) y su respuesta,
 * para depurar la integracion. Apagado por defecto: se enciende con {@code MP_LOG_LEVEL=DEBUG}.
 *
 * <p><b>Nunca loguea un secreto completo.</b> El token del vendedor, el token de la tarjeta, el
 * client_secret, el code de OAuth y los tokens que devuelve OAuth se recortan a sus primeros
 * caracteres: alcanza para saber DE QUIEN es (APP_USR- / TEST-) sin poder usarlo. Un log con
 * credenciales es peor que no tener log: los logs se copian, se pegan y se guardan.
 */
public class MercadoPagoHttpLog implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoHttpLog.class);

    private static final Pattern SECRETOS = Pattern.compile(
            "\"(token|client_secret|access_token|refresh_token)\"\\s*:\\s*\"([^\"]*)\"");
    /** Solo en el canje de OAuth: en los errores de MP "code" es un codigo de error util, no un secreto. */
    private static final Pattern CODE_OAUTH = Pattern.compile("\"(code)\"\\s*:\\s*\"([^\"]*)\"");

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (!log.isDebugEnabled()) {
            return execution.execute(request, body);
        }
        boolean oauth = request.getURI().getPath().startsWith("/oauth/");
        log.debug(">>> Mercado Pago {} {}\n    headers: {}\n    body: {}",
                request.getMethod(), request.getURI(), headers(request.getHeaders()),
                ocultar(new String(body, StandardCharsets.UTF_8), oauth));

        ClientHttpResponse response = execution.execute(request, body);
        // La fabrica del RestClient es "buffering" cuando este log esta activo: leer el body
        // aca no lo consume para el que despues lo deserializa.
        String respuesta = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        log.debug("<<< Mercado Pago {} {} -> {}\n    body: {}",
                request.getMethod(), request.getURI(), response.getStatusCode(), ocultar(respuesta, false));
        return response;
    }

    private static String headers(HttpHeaders headers) {
        StringBuilder sb = new StringBuilder();
        headers.forEach((nombre, valores) -> {
            String valor = String.join(",", valores);
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(nombre)) {
                valor = valor.startsWith("Bearer ") ? "Bearer " + recortar(valor.substring(7)) : recortar(valor);
            }
            sb.append(nombre).append('=').append(valor).append("; ");
        });
        return sb.toString();
    }

    static String ocultar(String json, boolean oauth) {
        if (json == null || json.isEmpty()) {
            return "(vacio)";
        }
        String salida = reemplazar(SECRETOS, json);
        return oauth ? reemplazar(CODE_OAUTH, salida) : salida;
    }

    private static String reemplazar(Pattern patron, String texto) {
        Matcher m = patron.matcher(texto);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement("\"" + m.group(1) + "\":\"" + recortar(m.group(2)) + "\""));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Lo justo para reconocer el tipo (APP_USR-/TEST-/TG-) sin que sirva para operar. */
    static String recortar(String secreto) {
        return secreto.length() <= 8 ? "***" : secreto.substring(0, 8) + "…(" + secreto.length() + " caracteres)";
    }
}
