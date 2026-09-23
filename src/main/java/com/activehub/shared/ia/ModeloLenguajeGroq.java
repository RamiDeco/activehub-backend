package com.activehub.shared.ia;

import com.activehub.shared.error.IaNoDisponibleException;
import com.activehub.shared.error.IaSinCuotaException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Groq Cloud por su API de chat, que es compatible con la de OpenAI ({@code /chat/completions}).
 *
 * <h2>Por qué a mano y no con un SDK</h2>
 *
 * Mismo criterio que {@code GoogleIdTokenVerifier} y {@code AlmacenamientoSupabase}: acá se hace
 * <b>un</b> POST con un JSON de cinco campos y se lee un string de la respuesta. El SDK oficial
 * de Groq para Java no existe; el de OpenAI arrastra su propia capa HTTP y de JSON para eso.
 *
 * <h2>Nada de streaming</h2>
 *
 * Se pide la respuesta completa ({@code stream: false}). El widget de chat muestra un indicador
 * de "escribiendo…" mientras espera y el informe de beneficios se genera de una sola vez: un
 * canal SSE hasta el navegador sumaría un tipo de endpoint nuevo (y su manejo de errores a medio
 * camino) sin cambiar lo que el usuario ve.
 *
 * <h2>El timeout es corto y a propósito</h2>
 *
 * Detrás de esto hay una persona esperando frente a una burbuja de chat. Con el plan gratuito una
 * respuesta puede tardar, pero si tarda más que {@code timeout-seg} preferimos el 503 y la caída
 * al comportamiento anterior antes que dejar el request colgado: sin timeout, un proveedor que no
 * responde ocupa un hilo de Tomcat hasta el timeout del sistema operativo.
 *
 * <p>El cuerpo del error del proveedor <b>se loguea pero nunca se le muestra al usuario</b>: puede
 * incluir detalles de la cuenta o de la clave. Lo que ve el usuario es el mensaje único de
 * {@link IaNoDisponibleException}.
 */
public class ModeloLenguajeGroq implements ModeloLenguaje {

    private static final Logger log = LoggerFactory.getLogger(ModeloLenguajeGroq.class);

    /** "Please try again in 2m59.56s" — horas, minutos y segundos, cualquiera puede faltar. */
    private static final Pattern ESPERA_EN_TEXTO = Pattern.compile(
            "try again in (?:(\\d+)h)?(?:(\\d+)m)?(?:([\\d.]+)s)?", Pattern.CASE_INSENSITIVE);

    private final RestClient restClient;
    private final String apiKey;
    private final String modelo;
    private final String razonamiento;

    public ModeloLenguajeGroq(
            String url, String apiKey, String modelo, String razonamiento, Duration timeout) {
        // Sobre el HttpClient del JDK, el mismo que usa el almacenamiento de archivos. Los dos
        // timeouts son necesarios: el de conexión cubre "el host no contesta" y el de lectura
        // "conectó y se quedó pensando", que es el caso real de un modelo saturado.
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.razonamiento = razonamiento == null ? "" : razonamiento.trim();
    }

    @Override
    public boolean disponible() {
        return true;
    }

    @Override
    public String completar(List<MensajeIa> mensajes, double temperatura, int maxTokens) {
        List<Map<String, String>> turnos = new ArrayList<>(mensajes.size());
        for (MensajeIa m : mensajes) {
            Map<String, String> turno = new LinkedHashMap<>();
            turno.put("role", m.rol());
            turno.put("content", m.contenido());
            turnos.add(turno);
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("model", modelo);
        cuerpo.put("messages", turnos);
        cuerpo.put("temperature", temperatura);
        cuerpo.put("max_completion_tokens", maxTokens);
        cuerpo.put("stream", false);
        // Los modelos de razonamiento (la familia gpt-oss, que es la que hay disponible hoy) gastan
        // tokens "pensando" ANTES de escribir, y ese gasto sale del mismo presupuesto. Con el
        // esfuerzo por defecto se llevaban los 320 tokens enteros y la respuesta volvía vacía: el
        // primer pedido real contra Groq falló exactamente así. "low" deja lugar para la respuesta y
        // alcanza de sobra para contestar con un texto que ya está escrito en el manual.
        if (!razonamiento.isBlank()) {
            cuerpo.put("reasoning_effort", razonamiento);
        }

        JsonNode respuesta;
        try {
            respuesta = restClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpStatusCodeException e) {
            // El 429 se distingue de todo lo demás: no es "se rompió", es "se agotó la cuota del
            // plan gratuito y hay que esperar", y el usuario tiene que poder enterarse de cuánto.
            if (e.getStatusCode().value() == 429) {
                Duration espera = esperaDe(e);
                log.warn("Groq rechazó por cuota (429). Reintentar en {}s", espera.toSeconds());
                throw new IaSinCuotaException(
                        espera,
                        IaSinCuotaException.Motivo.CUOTA_DEL_PROVEEDOR,
                        "El asistente alcanzó su límite de uso por ahora.");
            }
            log.warn("Groq respondió {}", e.getStatusCode());
            throw new IaNoDisponibleException();
        } catch (RuntimeException e) {
            // Timeout, DNS, TLS, JSON inesperado: para quien está esperando son lo mismo.
            log.warn("Groq no respondió correctamente: {}", e.getMessage());
            throw new IaNoDisponibleException();
        }

        String texto = textoDe(respuesta);
        if (texto == null || texto.isBlank()) {
            // `finish_reason: length` acá significa casi siempre que el presupuesto se lo comió el
            // razonamiento: es el dato que hace falta para saber si hay que subir maxTokens o bajar
            // `reasoning_effort`, y sin loguearlo el síntoma es "no contesta y no se sabe por qué".
            log.warn("Groq devolvió una respuesta sin contenido (finish_reason: {})",
                    respuesta == null ? "?" : respuesta.path("choices").path(0).path("finish_reason"));
            throw new IaNoDisponibleException();
        }
        return texto.trim();
    }

    /**
     * Cuánto hay que esperar, según el propio proveedor.
     *
     * <p>Groq manda el header {@code retry-after} (segundos, a veces con decimales) y, además, lo
     * escribe en el cuerpo: <i>"Please try again in 2m59.56s"</i>. Se mira primero el header, que es
     * el contrato, y el cuerpo sólo como respaldo. Si no hay ninguno de los dos se asume un minuto:
     * es mejor pedirle a la persona que vuelva en un minuto y que ya funcione, que decirle "no sé".
     */
    static Duration esperaDe(HttpStatusCodeException e) {
        HttpHeaders headers = e.getResponseHeaders();
        String header = headers == null ? null : headers.getFirst("retry-after");
        Duration delHeader = segundosDe(header);
        if (delHeader != null) {
            return delHeader;
        }
        Duration delCuerpo = delMensaje(e.getResponseBodyAsString());
        return delCuerpo != null ? delCuerpo : Duration.ofMinutes(1);
    }

    private static Duration segundosDe(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            // Se redondea hacia arriba: con 0,4 segundos, "esperá 0" sería volver a chocarse.
            return Duration.ofSeconds((long) Math.ceil(Double.parseDouble(valor.trim())));
        } catch (NumberFormatException ignorado) {
            return null;
        }
    }

    /** "…try again in 2m59.56s", "…in 45.5s", "…in 1h2m3s". */
    private static Duration delMensaje(String cuerpo) {
        if (cuerpo == null) {
            return null;
        }
        Matcher m = ESPERA_EN_TEXTO.matcher(cuerpo);
        if (!m.find()) {
            return null;
        }
        long horas = m.group(1) == null ? 0 : Long.parseLong(m.group(1));
        long minutos = m.group(2) == null ? 0 : Long.parseLong(m.group(2));
        double segundos = m.group(3) == null ? 0 : Double.parseDouble(m.group(3));
        Duration total = Duration.ofHours(horas)
                .plusMinutes(minutos)
                .plusSeconds((long) Math.ceil(segundos));
        return total.isZero() ? null : total;
    }

    /** {@code choices[0].message.content}, sin explotar si falta cualquiera de los tres. */
    private String textoDe(JsonNode respuesta) {
        if (respuesta == null) {
            return null;
        }
        JsonNode choices = respuesta.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }
        JsonNode contenido = choices.get(0).path("message").path("content");
        return contenido.isTextual() ? contenido.asString() : null;
    }

    @Override
    public String descripcion() {
        return "Groq (" + modelo + ")";
    }
}
