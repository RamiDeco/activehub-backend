package com.activehub.shared.payments;

import com.activehub.shared.error.ValidacionException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * El {@code state} del OAuth de Mercado Pago: identifica al instructor en el callback, que
 * llega desde el navegador SIN JWT, y es la defensa anti-CSRF del flujo.
 *
 * <p>Formato: {@code base64url(instructorId|expiraEpochSeg|nonce).base64url(HMAC-SHA256)}.
 *
 * <p><b>No es un JWT a proposito.</b> El state viaja en URLs (historial, logs del proxy, el
 * propio MP); si tuviera la forma de un token de sesion firmado con la misma clave,
 * {@code JwtAuthenticationFilter} lo aceptaria como {@code Bearer}. Por eso la clave HMAC
 * se DERIVA de {@code app.jwt.secret} con una etiqueta propia: un state no valida como JWT y
 * un JWT no valida como state.
 *
 * <p>Vence a los {@link #VIGENCIA} (el codigo de MP dura 10 minutos: no hace falta mas).
 */
@Component
public class MercadoPagoOAuthState {

    static final Duration VIGENCIA = Duration.ofMinutes(15);
    private static final String ALGORITMO = "HmacSHA256";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final SecretKeySpec clave;
    private final Clock clock;

    public MercadoPagoOAuthState(@Value("${app.jwt.secret}") String jwtSecret, Clock clock) {
        this.clave = new SecretKeySpec(
                hmac(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), ALGORITMO),
                        "mercadopago-oauth-state".getBytes(StandardCharsets.UTF_8)),
                ALGORITMO);
        this.clock = clock;
    }

    public String firmar(UUID instructorId) {
        long expira = clock.instant().plus(VIGENCIA).getEpochSecond();
        String payload = instructorId + "|" + expira + "|" + UUID.randomUUID();
        byte[] datos = payload.getBytes(StandardCharsets.UTF_8);
        return B64.encodeToString(datos) + "." + B64.encodeToString(hmac(clave, datos));
    }

    /**
     * @return el instructor que inicio el flujo
     * @throws ValidacionException (400) si el state falta, esta alterado o vencio
     */
    public UUID verificar(String state) {
        try {
            String[] partes = state.split("\\.");
            if (partes.length != 2) {
                throw invalido();
            }
            byte[] datos = B64D.decode(partes[0]);
            if (!MessageDigest.isEqual(hmac(clave, datos), B64D.decode(partes[1]))) {
                throw invalido();
            }
            String[] campos = new String(datos, StandardCharsets.UTF_8).split("\\|");
            if (campos.length != 3) {
                throw invalido();
            }
            if (clock.instant().getEpochSecond() > Long.parseLong(campos[1])) {
                throw new ValidacionException("La vinculación con Mercado Pago venció. Volvé a intentarlo desde tu perfil.");
            }
            return UUID.fromString(campos[0]);
        } catch (ValidacionException e) {
            throw e;
        } catch (RuntimeException e) {
            // null, base64 roto, numero o UUID mal formados: todo es "no lo emitimos nosotros".
            throw invalido();
        }
    }

    private static ValidacionException invalido() {
        return new ValidacionException("La vinculación con Mercado Pago no es válida. Volvé a intentarlo desde tu perfil.");
    }

    private static byte[] hmac(SecretKeySpec key, byte[] datos) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(key);
            return mac.doFinal(datos);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC no disponible", e);
        }
    }
}
