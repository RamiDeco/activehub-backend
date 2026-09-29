package com.activehub.usecases.obtenercuentamercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructorRepository;
import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructorRepository.ResumenCuenta;
import com.activehub.domain.mercadopago.EstadoCuentaMercadoPago;
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
class ObtenerCuentaMercadoPagoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-09-29T12:00:00Z");

    @Mock
    private CuentaMercadoPagoInstructorRepository cuentaRepository;

    private ObtenerCuentaMercadoPagoService service;
    private final UUID instructorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ObtenerCuentaMercadoPagoService(cuentaRepository, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static ResumenCuenta resumen(EstadoCuentaMercadoPago estado, Instant expira) {
        return new ResumenCuenta() {
            public String getMpUserId() { return "987654"; }
            public EstadoCuentaMercadoPago getEstado() { return estado; }
            public Instant getActualizadaEl() { return AHORA.minus(Duration.ofDays(1)); }
            public Instant getExpiraEl() { return expira; }
            public String getPublicKey() { return "TEST-pk"; }
        };
    }

    @Test
    void nuncaVinculo_noVinculadaYSinDatos() {
        when(cuentaRepository.findResumenByInstructorId(instructorId)).thenReturn(Optional.empty());

        var r = service.obtener(instructorId);

        assertThat(r.vinculada()).isFalse();
        assertThat(r.estado()).isNull();
        assertThat(r.mpUserId()).isNull();
    }

    @Test
    void vinculadaYVigente() {
        when(cuentaRepository.findResumenByInstructorId(instructorId))
                .thenReturn(Optional.of(resumen(EstadoCuentaMercadoPago.VINCULADA, AHORA.plus(Duration.ofDays(90)))));

        var r = service.obtener(instructorId);

        assertThat(r.vinculada()).isTrue();
        assertThat(r.estado()).isEqualTo("VINCULADA");
        assertThat(r.mpUserId()).isEqualTo("987654");
    }

    @Test
    void vinculadaPeroConElTokenVencido_seInformaComoVencida() {
        when(cuentaRepository.findResumenByInstructorId(instructorId))
                .thenReturn(Optional.of(resumen(EstadoCuentaMercadoPago.VINCULADA, AHORA.minusSeconds(1))));

        var r = service.obtener(instructorId);

        assertThat(r.vinculada()).isFalse();
        assertThat(r.estado()).isEqualTo("TOKEN_VENCIDO");
    }

    @Test
    void desvinculada() {
        when(cuentaRepository.findResumenByInstructorId(instructorId))
                .thenReturn(Optional.of(resumen(EstadoCuentaMercadoPago.DESVINCULADA, null)));

        assertThat(service.obtener(instructorId).vinculada()).isFalse();
    }
}
