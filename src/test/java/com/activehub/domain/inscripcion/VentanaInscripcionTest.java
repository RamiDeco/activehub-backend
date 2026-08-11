package com.activehub.domain.inscripcion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VentanaInscripcionTest {

    private static final Instant AHORA = Instant.parse("2026-08-01T00:00:00Z");

    @Test
    void masDe4Dias_soloPreinscripcion() {
        Instant fechaHora = AHORA.plus(Duration.ofDays(4).plusSeconds(1));
        assertThat(VentanaInscripcion.esVentanaPreInscripcion(AHORA, fechaHora)).isTrue();
        assertThat(VentanaInscripcion.esVentanaInscripcion(AHORA, fechaHora)).isFalse();
    }

    @Test
    void exactamente4Dias_yaEsInscripcionDefinitiva() {
        Instant fechaHora = AHORA.plus(Duration.ofDays(4));
        assertThat(VentanaInscripcion.esVentanaPreInscripcion(AHORA, fechaHora)).isFalse();
        assertThat(VentanaInscripcion.esVentanaInscripcion(AHORA, fechaHora)).isTrue();
    }

    @Test
    void entre1HoraY4Dias_esInscripcionDefinitiva() {
        Instant fechaHora = AHORA.plus(Duration.ofDays(2));
        assertThat(VentanaInscripcion.esVentanaInscripcion(AHORA, fechaHora)).isTrue();
    }

    @Test
    void exactamente1Hora_todaviaEsInscripcion() {
        Instant fechaHora = AHORA.plus(Duration.ofHours(1));
        assertThat(VentanaInscripcion.esVentanaInscripcion(AHORA, fechaHora)).isTrue();
    }

    @Test
    void menosDe1Hora_yaNoEsInscripcion() {
        Instant fechaHora = AHORA.plus(Duration.ofMinutes(59));
        assertThat(VentanaInscripcion.esVentanaInscripcion(AHORA, fechaHora)).isFalse();
        assertThat(VentanaInscripcion.esVentanaPreInscripcion(AHORA, fechaHora)).isFalse();
    }

    @Test
    void claseYaEmpezada_niInscripcionNiPreinscripcion() {
        Instant fechaHora = AHORA.minus(Duration.ofMinutes(5));
        assertThat(VentanaInscripcion.esVentanaInscripcion(AHORA, fechaHora)).isFalse();
        assertThat(VentanaInscripcion.esVentanaPreInscripcion(AHORA, fechaHora)).isFalse();
    }
}
