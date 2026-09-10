package com.activehub.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Reglas de seguridad de la sección 4: validación anti-inyección en las entradas.
 *
 * <p>La inyección SQL ya está cubierta por construcción — todas las consultas son JPQL o
 * nativas con parámetros nombrados, nunca concatenación — así que lo que falta chequear es
 * el XSS almacenado.
 */
class SinHtmlValidatorTest {

    private final SinHtmlValidator validator = new SinHtmlValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>",
            "<img src=x onerror=alert(1)>",
            "hola <b>mundo</b>",
            "texto normal y después <iframe src='http://malo'></iframe>",
            "<SCRIPT >alert(1)</SCRIPT>",
            "javascript:alert(1)",
            "JavaScript : alert(1)",
            "data:text/html;base64,PHNjcmlwdD4=",
    })
    void rechaza_marcadoYProtocolosPeligrosos(String valor) {
        assertThat(validator.isValid(valor, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Salidas grupales de running por el Parque General San Martín.",
            "El instructor no se presentó y no avisó.",
            // Un `<` suelto es texto legítimo: rechazarlo genera falsos positivos.
            "Grupos de cupo < 10 personas",
            "Precio > $5000 por clase",
            "Yoga & pilates",
            "Nivel: 3/5 — apto principiantes",
            "a < b y b > c",
    })
    void acepta_textoLegitimoAunqueTengaSignosDeComparacion(String valor) {
        assertThat(validator.isValid(valor, null)).isTrue();
    }

    @Test
    void nullYVacio_losDejaPasar_esProblemaDeNotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }
}
