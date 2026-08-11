package com.activehub.usecases.cancelarinscripcion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.MetodoPago;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.payments.PaymentGateway;
import java.math.BigDecimal;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CancelarInscripcionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-08-01T00:00:00Z");

    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private AuditService auditService;

    private CancelarInscripcionService service;
    private UUID inscripcionId;
    private UUID alumnoId;
    private UUID claseId;
    private Inscripcion inscripcion;
    private Clase clase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        service = new CancelarInscripcionService(inscripcionRepository, claseRepository, pagoRepository, paymentGateway, auditService, clock);

        alumnoId = UUID.randomUUID();
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        claseId = UUID.randomUUID();
        clase = new Clase();
        clase.setFechaHora(AHORA.plus(Duration.ofDays(2)));
        ReflectionTestUtils.setField(clase, "id", claseId);

        inscripcionId = UUID.randomUUID();
        inscripcion = new Inscripcion();
        inscripcion.setAlumno(alumno);
        inscripcion.setClase(clase);
        inscripcion.setEstado(EstadoInscripcion.INSCRIPTO);
        ReflectionTestUtils.setField(inscripcion, "id", inscripcionId);
    }

    @Test
    void cancelar_inscripto_liberaCupo() {
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        service.cancelar(inscripcionId, alumnoId);

        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
        verify(claseRepository).liberarCupo(claseId);
    }

    @Test
    void cancelar_preinscripcion_noLiberaCupo() {
        inscripcion.setEstado(EstadoInscripcion.PRE_INSCRIPCION);
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        service.cancelar(inscripcionId, alumnoId);

        verify(claseRepository, never()).liberarCupo(any());
    }

    @Test
    void cancelar_pagoRetenido_loMarcaCanceladoYLlamaGateway() {
        Pago pago = new Pago();
        pago.setEstado(EstadoPago.Retenido);
        pago.setMetodo(MetodoPago.MERCADO_PAGO);
        pago.setMonto(new BigDecimal("4500"));
        pago.setReferenciaExterna("ref-123");
        inscripcion.setPago(pago);

        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        service.cancelar(inscripcionId, alumnoId);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Cancelado);
        verify(paymentGateway).cancelarPago("ref-123");
        verify(pagoRepository).save(pago);
    }

    @Test
    void cancelar_pagoEfectivo_noLoToca() {
        Pago pago = new Pago();
        pago.setEstado(EstadoPago.Efectivo);
        pago.setMetodo(MetodoPago.EFECTIVO);
        pago.setMonto(new BigDecimal("4500"));
        inscripcion.setEstado(EstadoInscripcion.PAGO_PENDIENTE);
        inscripcion.setPago(pago);

        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        service.cancelar(inscripcionId, alumnoId);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Efectivo);
        verify(paymentGateway, never()).cancelarPago(any());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void cancelar_noDueño_lanzaSinPermiso() {
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        assertThatThrownBy(() -> service.cancelar(inscripcionId, UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void cancelar_yaCancelada_lanzaValidacion() {
        inscripcion.setEstado(EstadoInscripcion.CANCELADA);
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        assertThatThrownBy(() -> service.cancelar(inscripcionId, alumnoId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void cancelar_claseYaPaso_lanzaValidacion() {
        clase.setFechaHora(AHORA.minus(Duration.ofDays(1)));
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        assertThatThrownBy(() -> service.cancelar(inscripcionId, alumnoId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void cancelar_inexistente_lanzaNoEncontrado() {
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelar(inscripcionId, alumnoId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
