package com.activehub.usecases.liberarpagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.denuncia.EstadoDenuncia;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.inscripcion.VentanaPagos;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.payments.PaymentGateway;
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
class LiberarPagosServiceTest {

    private static final Instant AHORA = Instant.parse("2026-06-01T12:00:00Z");

    @Mock private PagoRepository pagoRepository;
    @Mock private DenunciaRepository denunciaRepository;
    @Mock private PaymentGateway paymentGateway;
    @Mock private AuditService auditService;

    private LiberarPagosService service;
    private UUID claseId;
    private Clase clase;
    private Pago pago;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        service = new LiberarPagosService(pagoRepository, denunciaRepository, paymentGateway, auditService, clock);

        claseId = UUID.randomUUID();
        clase = new Clase();
        clase.setEstado(EstadoClase.Finalizada);
        // Ya paso holgadamente el periodo de denuncias.
        clase.setFechaHora(AHORA.minus(VentanaPagos.PERIODO_DENUNCIAS).minus(Duration.ofHours(1)));
        ReflectionTestUtils.setField(clase, "id", claseId);

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setClase(clase);
        inscripcion.setEstado(EstadoInscripcion.INSCRIPTO);

        pago = new Pago();
        pago.setInscripcion(inscripcion);
        pago.setEstado(EstadoPago.Retenido);
        pago.setReferenciaExterna("ref-mock-1");
        ReflectionTestUtils.setField(pago, "id", UUID.randomUUID());
    }

    private void hayUnPagoRetenido() {
        when(pagoRepository.findByEstadoConClase(EstadoPago.Retenido)).thenReturn(List.of(pago));
    }

    @Test
    void liberar_claseFinalizadaYPeriodoVencido_acreditaAlInstructor() {
        hayUnPagoRetenido();
        when(denunciaRepository.existsByClaseIdAndEstadoNot(claseId, EstadoDenuncia.RESUELTA)).thenReturn(false);

        int liberados = service.liberar();

        assertThat(liberados).isEqualTo(1);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Liberado);
        verify(paymentGateway).confirmarPago("ref-mock-1");
        verify(pagoRepository).save(pago);
        verify(auditService).registrar(isNull(), eq(AuditAccion.PAGO_LIBERADO), eq("Pago"), any(), isNull());
    }

    @Test
    void liberar_periodoDeDenunciasTodaviaVigente_noLibera() {
        clase.setFechaHora(AHORA.minus(Duration.ofHours(1)));
        hayUnPagoRetenido();

        int liberados = service.liberar();

        assertThat(liberados).isZero();
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Retenido);
        verify(paymentGateway, never()).confirmarPago(any());
    }

    @Test
    void liberar_conDenunciaAbierta_retieneElPago() {
        hayUnPagoRetenido();
        when(denunciaRepository.existsByClaseIdAndEstadoNot(claseId, EstadoDenuncia.RESUELTA)).thenReturn(true);

        int liberados = service.liberar();

        assertThat(liberados).isZero();
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Retenido);
        verify(paymentGateway, never()).confirmarPago(any());
    }

    @Test
    void liberar_claseNoFinalizada_noLibera() {
        clase.setEstado(EstadoClase.Programada);
        hayUnPagoRetenido();

        int liberados = service.liberar();

        assertThat(liberados).isZero();
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Retenido);
    }

    @Test
    void liberar_inscripcionCancelada_noLibera() {
        pago.getInscripcion().setEstado(EstadoInscripcion.CANCELADA);
        hayUnPagoRetenido();

        int liberados = service.liberar();

        assertThat(liberados).isZero();
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Retenido);
    }
}
