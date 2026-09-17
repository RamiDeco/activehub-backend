package com.activehub.usecases.notificarausenciaprofesor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.activehub.shared.security.InstructorVerificadoGuard;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
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
class NotificarAusenciaProfesorServiceTest {

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private AuditService auditService;

    @org.mockito.Mock private InstructorVerificadoGuard instructorVerificadoGuard;


    private NotificarAusenciaProfesorService service;
    private UUID instructorId;
    private UUID claseId;
    private Clase clase;
    private NotificarAusenciaProfesorRequest request;

    @BeforeEach
    void setUp() {
        service = new NotificarAusenciaProfesorService(
                claseRepository, inscripcionRepository, pagoRepository, paymentGateway, notificacionService, auditService, instructorVerificadoGuard);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setInstructor(instructor);
        actividad.setNombre("Yoga");

        claseId = UUID.randomUUID();
        clase = new Clase();
        clase.setActividad(actividad);
        clase.setEstado(EstadoClase.Programada);
        ReflectionTestUtils.setField(clase, "id", claseId);

        request = new NotificarAusenciaProfesorRequest("El instructor avisó que no podrá dar la clase.");
    }

    private Inscripcion inscripcionCon(UUID alumnoId, EstadoInscripcion estado, Pago pago) {
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setClase(clase);
        inscripcion.setAlumno(alumno);
        inscripcion.setEstado(estado);
        inscripcion.setPago(pago);
        ReflectionTestUtils.setField(inscripcion, "id", UUID.randomUUID());
        return inscripcion;
    }

    @Test
    void notificar_cancelaClaseYNotificaAlumnosConPagoRetenido() {
        UUID alumnoId = UUID.randomUUID();
        Pago pago = new Pago();
        pago.setEstado(EstadoPago.Retenido);
        pago.setMetodo(MetodoPago.MERCADO_PAGO);
        pago.setMonto(new BigDecimal("4500"));
        pago.setReferenciaExterna("ref-abc");
        Inscripcion inscripto = inscripcionCon(alumnoId, EstadoInscripcion.INSCRIPTO, pago);

        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA))
                .thenReturn(List.of(inscripto));

        NotificarAusenciaProfesorResponse response = service.notificar(claseId, instructorId, request);

        assertThat(response.estado()).isEqualTo("Cancelada");
        assertThat(response.alumnosNotificados()).isEqualTo(1);
        assertThat(clase.getEstado()).isEqualTo(EstadoClase.Cancelada);
        assertThat(inscripto.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Cancelado);
        verify(paymentGateway).cancelarPago("ref-abc");
        verify(notificacionService).notificar(
                eq(alumnoId), eq(TipoNotificacion.AUSENCIA_PROFESOR), eq(request.mensaje()), eq(claseId), eq(Destino.clase(claseId)));
    }

    @Test
    void notificar_sinInscripciones_noNotificaANadie() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA))
                .thenReturn(List.of());

        NotificarAusenciaProfesorResponse response = service.notificar(claseId, instructorId, request);

        assertThat(response.alumnosNotificados()).isEqualTo(0);
        verify(notificacionService, never()).notificar(any(), any(), any(), any(), any());
    }

    @Test
    void notificar_yaCancelada_lanzaValidacion() {
        clase.setEstado(EstadoClase.Cancelada);
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.notificar(claseId, instructorId, request))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void notificar_yaFinalizada_lanzaValidacion() {
        clase.setEstado(EstadoClase.Finalizada);
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.notificar(claseId, instructorId, request))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void notificar_noDueño_lanzaSinPermiso() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.notificar(claseId, UUID.randomUUID(), request))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void notificar_inexistente_lanzaNoEncontrado() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.notificar(claseId, instructorId, request))
                .isInstanceOf(NoEncontradoException.class);
    }
}
