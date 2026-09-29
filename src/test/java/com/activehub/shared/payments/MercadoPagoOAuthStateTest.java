package com.activehub.shared.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.activehub.shared.error.ValidacionException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MercadoPagoOAuthStateTest {

    private static final String SECRETO = "secreto-de-prueba-0123456789abcdef0123456789abcdef";
    private static final Instant AHORA = Instant.parse("2026-09-29T12:00:00Z");

    private final MercadoPagoOAuthState state = new MercadoPagoOAuthState(SECRETO, Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void firmarYVerificar_devuelveElInstructor() {
        UUID instructorId = UUID.randomUUID();

        assertThat(state.verificar(state.firmar(instructorId))).isEqualTo(instructorId);
    }

    @Test
    void cadaStateEsDistinto() {
        UUID id = UUID.randomUUID();
        assertThat(state.firmar(id)).isNotEqualTo(state.firmar(id));
    }

    @Test
    void payloadAlterado_esInvalido() {
        String firmado = state.firmar(UUID.randomUUID());
        String firma = firmado.split("\\.")[1];
        String otroPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                (UUID.randomUUID() + "|" + AHORA.plusSeconds(600).getEpochSecond() + "|x").getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> state.verificar(otroPayload + "." + firma))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("no es válida");
    }

    @Test
    void firmadoConOtraClave_esInvalido() {
        var otro = new MercadoPagoOAuthState("otro-secreto-0123456789abcdef0123456789abcdef", Clock.fixed(AHORA, ZoneOffset.UTC));

        assertThatThrownBy(() -> state.verificar(otro.firmar(UUID.randomUUID())))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void vencido_esInvalido() {
        String firmado = state.firmar(UUID.randomUUID());
        var mas_tarde = new MercadoPagoOAuthState(SECRETO,
                Clock.fixed(AHORA.plus(MercadoPagoOAuthState.VIGENCIA).plusSeconds(1), ZoneOffset.UTC));

        assertThatThrownBy(() -> mas_tarde.verificar(firmado))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("venció");
    }

    @Test
    void basura_esInvalido() {
        for (String s : new String[] {null, "", "abc", "a.b.c", "%%%.###", "YWJj.YWJj"}) {
            assertThatThrownBy(() -> state.verificar(s)).as("state=%s", s).isInstanceOf(ValidacionException.class);
        }
    }

    @Test
    void unJwtDeSesion_noSirveComoState() {
        // Firmado con la MISMA clave de sesion: tiene que fallar porque la del state es derivada.
        String jwt = io.jsonwebtoken.Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> state.verificar(jwt)).isInstanceOf(ValidacionException.class);
    }
}
