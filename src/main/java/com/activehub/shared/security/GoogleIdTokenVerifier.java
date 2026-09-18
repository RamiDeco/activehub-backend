package com.activehub.shared.security;

import com.activehub.shared.error.CredencialesInvalidasException;
import com.activehub.shared.error.ValidacionException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import java.security.Key;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Valida el ID token que devuelve Google Identity Services en "Continuar con Google".
 *
 * <h2>Por qué a mano y no con `google-api-client`</h2>
 *
 * La librería oficial arrastra media docena de dependencias transitivas (su propio cliente
 * HTTP, Guava, su capa de JSON) para hacer exactamente esto: bajar unas claves públicas y
 * verificar una firma RS256. jjwt ya está en el proyecto para nuestros propios JWT y desde
 * 0.12 sabe parsear un JWK Set, así que alcanza con eso y un `RestClient`.
 *
 * <p>Tampoco se usa el endpoint `tokeninfo` de Google: implica un viaje de red **por cada
 * login**, y Google mismo recomienda validar localmente.
 *
 * <h2>Qué se verifica, y por qué cada cosa</h2>
 *
 * <ul>
 *   <li><b>Firma</b> contra las claves públicas de Google. Sin esto, cualquiera arma un JSON
 *       con el mail de otra persona y entra como ella: es el único control que importa.</li>
 *   <li><b>`aud` == nuestro client id.</b> Un token legítimo emitido para OTRA aplicación
 *       está correctamente firmado por Google; sin este chequeo serviría para entrar acá.</li>
 *   <li><b>`iss`</b> es de Google, y <b>`exp`</b> (con una tolerancia chica de reloj, que jjwt
 *       aplica sola).</li>
 *   <li><b>`email_verified`</b>. Google permite cuentas con correo sin confirmar; darlas por
 *       verificadas dejaría reservar un correo ajeno, que es justo lo que el código de 6
 *       dígitos existe para evitar.</li>
 * </ul>
 *
 * <p>Las claves se cachean: rotan cada pocos días y bajarlas en cada login sería un viaje de
 * red innecesario. Si la firma no valida con lo cacheado se refresca una vez y se reintenta —
 * así una rotación no voltea los logins hasta que expire el caché.
 */
@Component
public class GoogleIdTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenVerifier.class);

    private static final String JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> EMISORES = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final RestClient restClient = RestClient.create();
    private final AtomicReference<Cache> cache = new AtomicReference<>(null);
    private final String clientId;

    public GoogleIdTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId;
    }

    /** Sin client id configurado, "Continuar con Google" no está disponible. */
    public boolean habilitado() {
        return clientId != null && !clientId.isBlank();
    }

    public String clientId() {
        return clientId;
    }

    /**
     * @return los datos de la persona, ya verificados.
     * @throws ValidacionException          si Google no está configurado.
     * @throws CredencialesInvalidasException si el token no es válido para esta app.
     */
    public DatosGoogle verificar(String idToken) {
        if (!habilitado()) {
            throw new ValidacionException(
                    "El ingreso con Google no está configurado en este servidor.");
        }

        Claims claims = parsear(idToken, false)
                // Segundo intento con las claves recién bajadas: cubre la rotación de claves
                // de Google, que pasa sin aviso.
                .or(() -> parsear(idToken, true))
                .orElseThrow(CredencialesInvalidasException::new);

        if (!EMISORES.contains(String.valueOf(claims.getIssuer()))) {
            throw new CredencialesInvalidasException();
        }
        // `aud` es un Set en jjwt 0.12: el token puede declarar más de un destinatario.
        Set<String> audiencia = claims.getAudience();
        if (audiencia == null || !audiencia.contains(clientId)) {
            throw new CredencialesInvalidasException();
        }

        String email = claims.get("email", String.class);
        Boolean emailVerificado = claims.get("email_verified", Boolean.class);
        if (email == null || email.isBlank()) {
            throw new CredencialesInvalidasException();
        }
        if (!Boolean.TRUE.equals(emailVerificado)) {
            throw new ValidacionException(
                    "Tu cuenta de Google todavía no tiene el correo confirmado. "
                            + "Confirmalo en Google y volvé a intentar.");
        }

        return new DatosGoogle(
                claims.getSubject(),
                email.trim().toLowerCase(),
                textoOVacio(claims.get("given_name", String.class)),
                textoOVacio(claims.get("family_name", String.class)));
    }

    /** {@code Optional.empty()} si la firma no valida con las claves disponibles. */
    private Optional<Claims> parsear(String idToken, boolean refrescar) {
        try {
            Map<String, PublicKey> claves = claves(refrescar);
            return Optional.of(Jwts.parser()
                    // La clave sale del `kid` del header: Google publica varias a la vez
                    // porque las rota solapadas.
                    .keyLocator(header -> (Key) claves.get(String.valueOf(header.get("kid"))))
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload());
        } catch (Exception e) {
            if (refrescar) {
                log.debug("ID token de Google rechazado", e);
            }
            return Optional.empty();
        }
    }

    private Map<String, PublicKey> claves(boolean refrescar) {
        Cache actual = cache.get();
        if (!refrescar && actual != null && Instant.now().isBefore(actual.vence())) {
            return actual.claves();
        }
        JwkSet set = Jwks.setParser().build().parse(
                restClient.get().uri(JWKS_URL).retrieve().body(String.class));
        Map<String, PublicKey> claves = new java.util.HashMap<>();
        for (Jwk<?> jwk : set.getKeys()) {
            if (jwk.toKey() instanceof PublicKey publica && jwk.getId() != null) {
                claves.put(jwk.getId(), publica);
            }
        }
        cache.set(new Cache(Map.copyOf(claves), Instant.now().plus(CACHE_TTL)));
        return claves;
    }

    private static String textoOVacio(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private record Cache(Map<String, PublicKey> claves, Instant vence) {
    }

    /**
     * @param subject el id estable de la persona en Google. No se usa como clave todavía (la
     *                identidad de la cuenta es el correo), pero queda disponible.
     */
    public record DatosGoogle(String subject, String email, String nombre, String apellido) {
    }
}
