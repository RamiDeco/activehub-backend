package com.activehub.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class TokenCifradoConverterTest {

    private static String claveAleatoria() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private final TokenCifradoConverter converter = new TokenCifradoConverter(claveAleatoria());

    @Test
    void cifrarYDescifrarDevuelveElOriginal() {
        String token = "APP_USR-1234567890-abcdef-XYZ";

        String columna = converter.convertToDatabaseColumn(token);

        assertThat(columna).doesNotStartWith("APP_USR").doesNotContain(token);
        assertThat(converter.convertToEntityAttribute(columna)).isEqualTo(token);
    }

    @Test
    void mismoTokenCifraDistintoCadaVez() {
        String token = "TEST-token";
        assertThat(converter.convertToDatabaseColumn(token))
                .isNotEqualTo(converter.convertToDatabaseColumn(token));
    }

    @Test
    void nullQuedaNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void otraClaveNoDescifra() {
        String columna = converter.convertToDatabaseColumn("TEST-token");
        TokenCifradoConverter otro = new TokenCifradoConverter(claveAleatoria());

        assertThatThrownBy(() -> otro.convertToEntityAttribute(columna))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sinClaveNoGuardaEnClaro() {
        TokenCifradoConverter sinClave = new TokenCifradoConverter("");

        assertThatThrownBy(() -> sinClave.convertToDatabaseColumn("TEST-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MP_TOKEN_ENCRYPTION_KEY");
    }

    @Test
    void claveDeLargoIncorrectoFallaAlConstruir() {
        String corta = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new TokenCifradoConverter(corta))
                .isInstanceOf(IllegalStateException.class);
    }
}
