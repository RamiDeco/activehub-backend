package com.activehub.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class IntentosLoginServiceTest {

    private static final Instant AHORA = Instant.parse("2026-06-01T12:00:00Z");
    private static final String EMAIL = "alumno@activehub.test";

    private IntentosLoginService conReloj(Instant instante) {
        return new IntentosLoginService(Clock.fixed(instante, ZoneOffset.UTC));
    }

    @Test
    void noBloquea_pordebajoDelMaximo() {
        IntentosLoginService service = conReloj(AHORA);

        for (int i = 0; i < IntentosLoginService.MAX_INTENTOS - 1; i++) {
            service.registrarFallo(EMAIL);
        }

        assertThat(service.estaBloqueado(EMAIL)).isFalse();
    }

    @Test
    void bloquea_alLlegarAlMaximoDeIntentos() {
        IntentosLoginService service = conReloj(AHORA);

        for (int i = 0; i < IntentosLoginService.MAX_INTENTOS; i++) {
            service.registrarFallo(EMAIL);
        }

        assertThat(service.estaBloqueado(EMAIL)).isTrue();
    }

    @Test
    void bloqueo_esPorEmailYNoAfectaAOtros() {
        IntentosLoginService service = conReloj(AHORA);

        for (int i = 0; i < IntentosLoginService.MAX_INTENTOS; i++) {
            service.registrarFallo(EMAIL);
        }

        assertThat(service.estaBloqueado("otro@activehub.test")).isFalse();
    }

    @Test
    void bloqueo_ignoraMayusculasYEspacios() {
        IntentosLoginService service = conReloj(AHORA);

        for (int i = 0; i < IntentosLoginService.MAX_INTENTOS; i++) {
            service.registrarFallo("  ALUMNO@activehub.test ");
        }

        assertThat(service.estaBloqueado(EMAIL)).isTrue();
    }

    @Test
    void loginExitoso_limpiaElContador() {
        IntentosLoginService service = conReloj(AHORA);
        for (int i = 0; i < IntentosLoginService.MAX_INTENTOS - 1; i++) {
            service.registrarFallo(EMAIL);
        }

        service.registrarExito(EMAIL);
        service.registrarFallo(EMAIL);

        assertThat(service.estaBloqueado(EMAIL)).isFalse();
    }

    /** Reloj movible: permite avanzar el tiempo sobre la MISMA instancia del service. */
    private static final class RelojMovible extends Clock {
        private Instant instante;

        RelojMovible(Instant inicial) {
            this.instante = inicial;
        }

        void avanzar(Duration d) {
            instante = instante.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }

    @Test
    void bloqueo_seLevantaPasadoElPeriodo() {
        RelojMovible reloj = new RelojMovible(AHORA);
        IntentosLoginService service = new IntentosLoginService(reloj);
        for (int i = 0; i < IntentosLoginService.MAX_INTENTOS; i++) {
            service.registrarFallo(EMAIL);
        }
        assertThat(service.estaBloqueado(EMAIL)).isTrue();

        reloj.avanzar(IntentosLoginService.BLOQUEO.plus(Duration.ofMinutes(1)));

        assertThat(service.estaBloqueado(EMAIL)).isFalse();
    }

    @Test
    void fallosViejos_noSeAcumulanPasadaLaVentana() {
        RelojMovible reloj = new RelojMovible(AHORA);
        IntentosLoginService service = new IntentosLoginService(reloj);
        for (int i = 0; i < IntentosLoginService.MAX_INTENTOS - 1; i++) {
            service.registrarFallo(EMAIL);
        }

        // Pasada la ventana el contador se reinicia: un fallo aislado no debe bloquear.
        reloj.avanzar(IntentosLoginService.VENTANA.plus(Duration.ofMinutes(1)));
        service.registrarFallo(EMAIL);

        assertThat(service.estaBloqueado(EMAIL)).isFalse();
    }
}
