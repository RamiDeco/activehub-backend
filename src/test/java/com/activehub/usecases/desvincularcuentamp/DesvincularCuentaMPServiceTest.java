package com.activehub.usecases.desvincularcuentamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructorRepository;
import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructorRepository.ResumenCuenta;
import com.activehub.domain.mercadopago.EstadoCuentaMercadoPago;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.ApiErrorCode;
import com.activehub.shared.error.CuentaMercadoPagoConPagosPendientesException;
import com.activehub.shared.error.NoEncontradoException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DesvincularCuentaMPServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-10T12:00:00Z");
    private static final List<EstadoPago> PENDIENTES = List.of(EstadoPago.Retenido, EstadoPago.EnRevision);

    @Mock
    private CuentaMercadoPagoInstructorRepository cuentaRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private AuditService auditService;

    private DesvincularCuentaMPService service;
    private UUID instructorId;
    private UUID cuentaId;

    @BeforeEach
    void setUp() {
        service = new DesvincularCuentaMPService(cuentaRepository, pagoRepository, auditService,
                Clock.fixed(AHORA, ZoneOffset.UTC));
        instructorId = UUID.randomUUID();
        cuentaId = UUID.randomUUID();
    }

    private ResumenCuenta resumen(EstadoCuentaMercadoPago estado) {
        return new ResumenCuenta() {
            public UUID getId() { return cuentaId; }
            public String getMpUserId() { return "987654"; }
            public EstadoCuentaMercadoPago getEstado() { return estado; }
            public Instant getActualizadaEl() { return AHORA; }
            public Instant getExpiraEl() { return AHORA.plusSeconds(86400); }
            public String getPublicKey() { return "APP_USR-pk"; }
        };
    }

    @Test
    void sinPagosPendientes_desvinculaYAudita() {
        when(cuentaRepository.findResumenByInstructorId(instructorId))
                .thenReturn(Optional.of(resumen(EstadoCuentaMercadoPago.VINCULADA)));
        when(pagoRepository.countDeMercadoPagoDeInstructorEn(instructorId, PENDIENTES)).thenReturn(0L);

        service.desvincular(instructorId);

        verify(cuentaRepository).desvincular(instructorId, EstadoCuentaMercadoPago.DESVINCULADA, AHORA);
        verify(auditService).registrar(instructorId, AuditAccion.MERCADOPAGO_DESVINCULADA,
                "CuentaMercadoPagoInstructor", cuentaId, "mp_user_id=987654");
    }

    @Test
    void conPagosRetenidos_lanza409YNoTocaLaCuenta() {
        when(cuentaRepository.findResumenByInstructorId(instructorId))
                .thenReturn(Optional.of(resumen(EstadoCuentaMercadoPago.VINCULADA)));
        when(pagoRepository.countDeMercadoPagoDeInstructorEn(instructorId, PENDIENTES)).thenReturn(3L);

        assertThatThrownBy(() -> service.desvincular(instructorId))
                .isInstanceOf(CuentaMercadoPagoConPagosPendientesException.class)
                .hasMessageContaining("3 pagos")
                .satisfies(e -> assertThat(((CuentaMercadoPagoConPagosPendientesException) e).getCode())
                        .isEqualTo(ApiErrorCode.MERCADOPAGO_CON_PAGOS_PENDIENTES));

        verify(cuentaRepository, never()).desvincular(any(), any(), any());
        verifyNoInteractions(auditService);
    }

    @Test
    void conUnSoloPagoPendiente_elMensajeVaEnSingular() {
        when(cuentaRepository.findResumenByInstructorId(instructorId))
                .thenReturn(Optional.of(resumen(EstadoCuentaMercadoPago.VINCULADA)));
        when(pagoRepository.countDeMercadoPagoDeInstructorEn(instructorId, PENDIENTES)).thenReturn(1L);

        assertThatThrownBy(() -> service.desvincular(instructorId))
                .hasMessageContaining("hay 1 pago de un alumno que todavía se cobra");
    }

    @Test
    void conTokenVencido_tambienSePuedeDesvincular() {
        when(cuentaRepository.findResumenByInstructorId(instructorId))
                .thenReturn(Optional.of(resumen(EstadoCuentaMercadoPago.TOKEN_VENCIDO)));
        when(pagoRepository.countDeMercadoPagoDeInstructorEn(instructorId, PENDIENTES)).thenReturn(0L);

        service.desvincular(instructorId);

        verify(cuentaRepository).desvincular(instructorId, EstadoCuentaMercadoPago.DESVINCULADA, AHORA);
    }

    @Test
    void yaDesvinculada_noHaceNada() {
        when(cuentaRepository.findResumenByInstructorId(instructorId))
                .thenReturn(Optional.of(resumen(EstadoCuentaMercadoPago.DESVINCULADA)));

        service.desvincular(instructorId);

        verify(cuentaRepository, never()).desvincular(any(), any(), any());
        verifyNoInteractions(pagoRepository, auditService);
    }

    @Test
    void sinCuenta_lanzaNoEncontrado() {
        when(cuentaRepository.findResumenByInstructorId(instructorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desvincular(instructorId)).isInstanceOf(NoEncontradoException.class);
        verifyNoInteractions(pagoRepository, auditService);
    }
}
