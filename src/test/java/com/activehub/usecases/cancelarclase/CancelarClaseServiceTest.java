package com.activehub.usecases.cancelarclase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CancelarClaseServiceTest {

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private AuditService auditService;

    private CancelarClaseService service;
    private UUID instructorId;
    private UUID claseId;
    private Clase clase;

    @BeforeEach
    void setUp() {
        service = new CancelarClaseService(claseRepository, inscripcionRepository, pagoRepository, paymentGateway, auditService);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setInstructor(instructor);

        claseId = UUID.randomUUID();
        clase = new Clase();
        clase.setActividad(actividad);
        clase.setEstado(EstadoClase.Programada);
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
    void cancelar_cancelaInscripcionesYPagosRetenidos() {
        Pago pago = new Pago();
        pago.setEstado(EstadoPago.Retenido);
        pago.setMetodo(MetodoPago.MERCADO_PAGO);
        pago.setMonto(new BigDecimal("4500"));
        pago.setReferenciaExterna("ref-abc");
        Inscripcion inscripto = inscripcionCon(EstadoInscripcion.INSCRIPTO, pago);

        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA))
                .thenReturn(List.of(inscripto));

        CancelarClaseResponse response = service.cancelar(claseId, instructorId);

        assertThat(response.estado()).isEqualTo("Cancelada");
        assertThat(clase.getEstado()).isEqualTo(EstadoClase.Cancelada);
        assertThat(inscripto.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Cancelado);
        verify(paymentGateway).cancelarPago("ref-abc");
    }

    @Test
    void cancelar_pagoEfectivo_noLoToca() {
        Pago pago = new Pago();
        pago.setEstado(EstadoPago.Efectivo);
        pago.setMetodo(MetodoPago.EFECTIVO);
        pago.setMonto(new BigDecimal("4500"));
        Inscripcion pendiente = inscripcionCon(EstadoInscripcion.PAGO_PENDIENTE, pago);

        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA))
                .thenReturn(List.of(pendiente));

        service.cancelar(claseId, instructorId);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Efectivo);
        assertThat(pendiente.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
        verify(paymentGateway, never()).cancelarPago(any());
    }

    @Test
    void cancelar_yaCancelada_lanzaValidacion() {
        clase.setEstado(EstadoClase.Cancelada);
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.cancelar(claseId, instructorId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void cancelar_yaFinalizada_lanzaValidacion() {
        clase.setEstado(EstadoClase.Finalizada);
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.cancelar(claseId, instructorId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void cancelar_noDueño_lanzaSinPermiso() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.cancelar(claseId, UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void cancelar_inexistente_lanzaNoEncontrado() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelar(claseId, instructorId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
