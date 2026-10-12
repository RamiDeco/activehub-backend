package com.activehub.shared.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.activehub.shared.error.ApiErrorCode;
import com.activehub.shared.error.IaSinCuotaException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LimiteConsultasIaTest {

    private static final Instant AHORA = Instant.parse("2026-03-10T12:00:00Z");

    @Test
    void registrar_dentroDelLimite_pasa() {
        LimiteConsultasIa limite = new LimiteConsultasIa(Clock.fixed(AHORA, ZoneOffset.UTC));

        for (int i = 0; i < LimiteConsultasIa.MAX_POR_MINUTO; i++) {
            assertThatCode(() -> limite.registrar("ip:1.2.3.4")).doesNotThrowAnyException();
        }
    }

    @Test
    void registrar_pasadoElLimitePorMinuto_rechazaCon429YDiceCuantoFalta() {
        RelojMovil reloj = new RelojMovil(AHORA);
        LimiteConsultasIa limite = new LimiteConsultasIa(reloj);
        for (int i = 0; i < LimiteConsultasIa.MAX_POR_MINUTO; i++) {
            limite.registrar("ip:1.2.3.4");
        }
        // 20 segundos después de la ráfaga: falta que pase el resto del minuto.
        reloj.avanzar(Duration.ofSeconds(20));

        IaSinCuotaException e = catchThrowableOfType(
                () -> limite.registrar("ip:1.2.3.4"), IaSinCuotaException.class);

        assertThat(e.getCode()).isEqualTo(ApiErrorCode.IA_SIN_CUOTA);
        assertThat(e.getMotivo()).isEqualTo(IaSinCuotaException.Motivo.USO_INTENSO);
        // La espera se calcula sobre la ventana, no es un número fijo: es lo que permite decirle a
        // la persona "volvé en X" en vez de "esperá un rato".
        assertThat(e.getEspera()).isEqualTo(Duration.ofSeconds(40));
    }

    @Test
    void registrar_elMensajeParaLaPantallaDiceQueHacerMientrasEspera() {
        LimiteConsultasIa limite = new LimiteConsultasIa(Clock.fixed(AHORA, ZoneOffset.UTC));
        for (int i = 0; i < LimiteConsultasIa.MAX_POR_MINUTO; i++) {
            limite.registrar("ip:1.2.3.4");
        }

        IaSinCuotaException e = catchThrowableOfType(
                () -> limite.registrar("ip:1.2.3.4"), IaSinCuotaException.class);

        assertThat(e.paraElAsistente().getMessage())
                .contains("descanso")
                .contains("un minuto")
                .contains("preguntas frecuentes");
        assertThat(e.paraElInforme().getMessage())
                .contains("informes")
                .contains("beneficios y prevenciones generales");
    }

    @Test
    void registrar_elLimiteEsPorClave_unClienteNoBloqueaAOtro() {
        LimiteConsultasIa limite = new LimiteConsultasIa(Clock.fixed(AHORA, ZoneOffset.UTC));
        for (int i = 0; i < LimiteConsultasIa.MAX_POR_MINUTO; i++) {
            limite.registrar("ip:1.2.3.4");
        }

        assertThatCode(() -> limite.registrar("usuario:otra-persona")).doesNotThrowAnyException();
    }

    @Test
    void registrar_alMinutoSiguiente_vuelveAPermitir() {
        // La ventana es deslizante: pasado el minuto las consultas viejas dejan de contar, sin
        // ningún job que las limpie.
        RelojMovil reloj = new RelojMovil(AHORA);
        LimiteConsultasIa limite = new LimiteConsultasIa(reloj);
        for (int i = 0; i < LimiteConsultasIa.MAX_POR_MINUTO; i++) {
            limite.registrar("ip:1.2.3.4");
        }

        reloj.avanzar(Duration.ofSeconds(61));

        assertThatCode(() -> limite.registrar("ip:1.2.3.4")).doesNotThrowAnyException();
    }

    @Test
    void registrar_pasadoElLimitePorHora_rechazaAunqueEsperandoEntreRafagas() {
        RelojMovil reloj = new RelojMovil(AHORA);
        LimiteConsultasIa limite = new LimiteConsultasIa(reloj);

        int hechas = 0;
        // Ráfagas separadas por un minuto: la ventana del minuto nunca se llena, así que lo único
        // que puede frenarlo es el tope por hora. Es el insistidor paciente.
        while (hechas < LimiteConsultasIa.MAX_POR_HORA) {
            limite.registrar("ip:1.2.3.4");
            hechas++;
            if (hechas % LimiteConsultasIa.MAX_POR_MINUTO == 0) {
                reloj.avanzar(Duration.ofSeconds(61));
            }
        }

        assertThatThrownBy(() -> limite.registrar("ip:1.2.3.4"))
                .isInstanceOf(IaSinCuotaException.class)
                .hasMessageContaining("muchas consultas seguidas");
        assertThat(hechas).isEqualTo(LimiteConsultasIa.MAX_POR_HORA);
    }

    /** Un {@link Clock} que se puede mover a mano: {@code Clock.fixed} no alcanza para ventanas. */
    private static final class RelojMovil extends Clock {

        private Instant ahora;

        private RelojMovil(Instant inicio) {
            this.ahora = inicio;
        }

        private void avanzar(Duration cuanto) {
            ahora = ahora.plus(cuanto);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
