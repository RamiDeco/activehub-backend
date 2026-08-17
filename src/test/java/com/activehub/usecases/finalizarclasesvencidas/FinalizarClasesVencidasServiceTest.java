package com.activehub.usecases.finalizarclasesvencidas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.MetodoPago;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FinalizarClasesVencidasServiceTest {

    private static final Instant AHORA = Instant.parse("2026-08-20T00:00:00Z");

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private AuditService auditService;

    private FinalizarClasesVencidasService service;
    private UUID claseId;
    private Clase clase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        service = new FinalizarClasesVencidasService(claseRepository, inscripcionRepository, auditService, clock);

        claseId = UUID.randomUUID();
        clase = new Clase();
        clase.setEstado(EstadoClase.Programada);
        clase.setFechaHora(AHORA.minus(Duration.ofDays(1)));
        ReflectionTestUtils.setField(clase, "id", claseId);
    }

    private Inscripcion inscripcionCon(EstadoInscripcion estado, Pago pago) {
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setClase(clase);
        inscripcion.setEstado(estado);
        inscripcion.setPago(pago);
        ReflectionTestUtils.setField(inscripcion, "id", UUID.randomUUID());
        return inscripcion;
    }

    @Test
    void finalizar_claseVencidaProgramada_pasaAFinalizada() {
        when(claseRepository.findByEstadoInAndFechaHoraBefore(List.of(EstadoClase.Programada, EstadoClase.Habilitada), AHORA))
                .thenReturn(List.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA)).thenReturn(List.of());

        service.finalizar();

        assertThat(clase.getEstado()).isEqualTo(EstadoClase.Finalizada);
        verify(auditService).registrar(isNull(), eq(AuditAccion.CLASE_FINALIZADA), eq("Clase"), eq(claseId), isNull());
    }

    @Test
    void finalizar_claseVencidaHabilitada_pasaAFinalizada() {
        clase.setEstado(EstadoClase.Habilitada);
        when(claseRepository.findByEstadoInAndFechaHoraBefore(List.of(EstadoClase.Programada, EstadoClase.Habilitada), AHORA))
                .thenReturn(List.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA)).thenReturn(List.of());

        service.finalizar();

        assertThat(clase.getEstado()).isEqualTo(EstadoClase.Finalizada);
    }

    @Test
    void finalizar_preInscripcion_pasaACancelada() {
        Inscripcion preInscripcion = inscripcionCon(EstadoInscripcion.PRE_INSCRIPCION, null);

        when(claseRepository.findByEstadoInAndFechaHoraBefore(any(), eq(AHORA))).thenReturn(List.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA))
                .thenReturn(List.of(preInscripcion));

        service.finalizar();

        assertThat(preInscripcion.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
    }

    @Test
    void finalizar_pagoPendienteEfectivo_pasaACanceladaSinTocarElPago() {
        Pago efectivo = new Pago();
        efectivo.setEstado(EstadoPago.Efectivo);
        efectivo.setMetodo(MetodoPago.EFECTIVO);
        efectivo.setMonto(new BigDecimal("4500"));
        Inscripcion pagoPendiente = inscripcionCon(EstadoInscripcion.PAGO_PENDIENTE, efectivo);

        when(claseRepository.findByEstadoInAndFechaHoraBefore(any(), eq(AHORA))).thenReturn(List.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA))
                .thenReturn(List.of(pagoPendiente));

        service.finalizar();

        assertThat(pagoPendiente.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
        assertThat(efectivo.getEstado()).isEqualTo(EstadoPago.Efectivo);
    }

    @Test
    void finalizar_inscripto_quedaIntactoSinTocarElPagoRetenido() {
        Pago retenido = new Pago();
        retenido.setEstado(EstadoPago.Retenido);
        retenido.setMetodo(MetodoPago.MERCADO_PAGO);
        retenido.setMonto(new BigDecimal("4500"));
        Inscripcion inscripto = inscripcionCon(EstadoInscripcion.INSCRIPTO, retenido);

        when(claseRepository.findByEstadoInAndFechaHoraBefore(any(), eq(AHORA))).thenReturn(List.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA))
                .thenReturn(List.of(inscripto));

        service.finalizar();

        assertThat(inscripto.getEstado()).isEqualTo(EstadoInscripcion.INSCRIPTO);
        assertThat(retenido.getEstado()).isEqualTo(EstadoPago.Retenido);
    }

    @Test
    void finalizar_sinClasesVencidas_noHaceNada() {
        when(claseRepository.findByEstadoInAndFechaHoraBefore(any(), eq(AHORA))).thenReturn(List.of());

        service.finalizar();

        verify(auditService, org.mockito.Mockito.never()).registrar(any(), any(), any(), any(), any());
    }
}
