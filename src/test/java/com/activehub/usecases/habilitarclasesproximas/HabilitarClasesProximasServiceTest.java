package com.activehub.usecases.habilitarclasesproximas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
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
class HabilitarClasesProximasServiceTest {

    private static final Instant AHORA = Instant.parse("2026-06-01T12:00:00Z");

    @Mock private ClaseRepository claseRepository;
    @Mock private AuditService auditService;

    private HabilitarClasesProximasService service;

    @BeforeEach
    void setUp() {
        service = new HabilitarClasesProximasService(
                claseRepository, auditService, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private Clase clase(EstadoClase estado, Instant fechaHora) {
        Clase c = new Clase();
        c.setEstado(estado);
        c.setFechaHora(fechaHora);
        ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
        return c;
    }

    @Test
    void habilitar_claseADosDias_pasaAHabilitada() {
        Clase c = clase(EstadoClase.Programada, AHORA.plus(Duration.ofDays(2)));
        when(claseRepository.findByEstadoInAndFechaHoraBefore(any(), any())).thenReturn(List.of(c));

        int habilitadas = service.habilitar();

        assertThat(habilitadas).isEqualTo(1);
        assertThat(c.getEstado()).isEqualTo(EstadoClase.Habilitada);
        verify(claseRepository).save(c);
        verify(auditService).registrar(isNull(), eq(AuditAccion.CLASE_HABILITADA), eq("Clase"), any(), isNull());
    }

    @Test
    void habilitar_claseYaVencida_noLaToca() {
        // Una clase pasada es problema del job de finalizacion, no de este.
        Clase c = clase(EstadoClase.Programada, AHORA.minus(Duration.ofHours(3)));
        when(claseRepository.findByEstadoInAndFechaHoraBefore(any(), any())).thenReturn(List.of(c));

        int habilitadas = service.habilitar();

        assertThat(habilitadas).isZero();
        assertThat(c.getEstado()).isEqualTo(EstadoClase.Programada);
        verify(claseRepository, never()).save(any());
    }

    @Test
    void habilitar_sinCandidatas_noHaceNada() {
        when(claseRepository.findByEstadoInAndFechaHoraBefore(any(), any())).thenReturn(List.of());

        assertThat(service.habilitar()).isZero();
        verify(auditService, never()).registrar(any(), any(), any(), any(), any());
    }
}
