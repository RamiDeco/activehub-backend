package com.activehub.shared.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructor;
import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructorRepository;
import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructorRepository.ResumenCuenta;
import com.activehub.domain.mercadopago.EstadoCuentaMercadoPago;
import com.activehub.shared.error.CuentaMercadoPagoNoVinculadaException;
import com.activehub.shared.error.PagoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CuentaMercadoPagoGuardTest {

    private static final Instant AHORA = Instant.parse("2026-09-29T12:00:00Z");

    @Mock
    private CuentaMercadoPagoInstructorRepository cuentaRepository;

    private CuentaMercadoPagoGuard guard;
    private final UUID instructorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        guard = new CuentaMercadoPagoGuard(cuentaRepository, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static ResumenCuenta resumen(EstadoCuentaMercadoPago estado, Instant expira, String publicKey) {
        return new ResumenCuenta() {
            public UUID getId() { return UUID.randomUUID(); }
            public String getMpUserId() { return "987654"; }
            public EstadoCuentaMercadoPago getEstado() { return estado; }
            public Instant getActualizadaEl() { return AHORA; }
            public Instant getExpiraEl() { return expira; }
            public String getPublicKey() { return publicKey; }
        };
    }

    private static CuentaMercadoPagoInstructor cuenta(EstadoCuentaMercadoPago estado, Instant expira) {
        var c = new CuentaMercadoPagoInstructor();
        c.setEstado(estado);
        c.setExpiresAt(expira);
        c.setAccessToken("TEST-seller-token");
        return c;
    }

    @Test
    void publicKey_cuentaVinculadaYVigente_laDevuelve() {
        when(cuentaRepository.findResumenByInstructorId(instructorId)).thenReturn(Optional.of(
                resumen(EstadoCuentaMercadoPago.VINCULADA, AHORA.plus(Duration.ofDays(90)), "TEST-pk")));

        assertThat(guard.publicKeySiAceptaMercadoPago(instructorId)).contains("TEST-pk");
    }

    @Test
    void publicKey_sinCuentaVencidaODesvinculada_vacio() {
        when(cuentaRepository.findResumenByInstructorId(instructorId)).thenReturn(Optional.empty());
        assertThat(guard.publicKeySiAceptaMercadoPago(instructorId)).isEmpty();

        when(cuentaRepository.findResumenByInstructorId(instructorId)).thenReturn(Optional.of(
                resumen(EstadoCuentaMercadoPago.VINCULADA, AHORA.minusSeconds(1), "TEST-pk")));
        assertThat(guard.publicKeySiAceptaMercadoPago(instructorId)).isEmpty();

        when(cuentaRepository.findResumenByInstructorId(instructorId)).thenReturn(Optional.of(
                resumen(EstadoCuentaMercadoPago.DESVINCULADA, null, "TEST-pk")));
        assertThat(guard.publicKeySiAceptaMercadoPago(instructorId)).isEmpty();
    }

    @Test
    void publicKey_vinculadaPeroSinPublicKey_noAcepta() {
        // Sin public key el Brick no puede tokenizar: ofrecer MP seria prometer algo que falla.
        when(cuentaRepository.findResumenByInstructorId(instructorId)).thenReturn(Optional.of(
                resumen(EstadoCuentaMercadoPago.VINCULADA, null, null)));

        assertThat(guard.publicKeySiAceptaMercadoPago(instructorId)).isEmpty();
    }

    @Test
    void exigir_cuentaVigente_devuelveElToken() {
        when(cuentaRepository.findByInstructorId(instructorId)).thenReturn(Optional.of(
                cuenta(EstadoCuentaMercadoPago.VINCULADA, AHORA.plus(Duration.ofDays(1)))));

        assertThat(guard.exigirCuentaVinculada(instructorId)).isEqualTo("TEST-seller-token");
    }

    @Test
    void exigir_sinCuentaVencidaODesvinculada_lanza409() {
        when(cuentaRepository.findByInstructorId(instructorId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> guard.exigirCuentaVinculada(instructorId))
                .isInstanceOf(CuentaMercadoPagoNoVinculadaException.class);

        when(cuentaRepository.findByInstructorId(instructorId)).thenReturn(Optional.of(
                cuenta(EstadoCuentaMercadoPago.VINCULADA, AHORA.minusSeconds(1))));
        assertThatThrownBy(() -> guard.exigirCuentaVinculada(instructorId))
                .isInstanceOf(CuentaMercadoPagoNoVinculadaException.class);

        when(cuentaRepository.findByInstructorId(instructorId)).thenReturn(Optional.of(
                cuenta(EstadoCuentaMercadoPago.TOKEN_VENCIDO, null)));
        assertThatThrownBy(() -> guard.exigirCuentaVinculada(instructorId))
                .isInstanceOf(CuentaMercadoPagoNoVinculadaException.class);
    }

    @Test
    void exigir_tokenQueNoSePuedeDescifrar_lanzaPagoException() {
        when(cuentaRepository.findByInstructorId(instructorId))
                .thenThrow(new IllegalStateException("Falta MP_TOKEN_ENCRYPTION_KEY"));

        assertThatThrownBy(() -> guard.exigirCuentaVinculada(instructorId)).isExactlyInstanceOf(PagoException.class);
    }
}
