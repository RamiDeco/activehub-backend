package com.activehub.usecases.eliminaractividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
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
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.payments.PaymentGateway;
import java.math.BigDecimal;
import java.time.Instant;
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
class EliminarActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;
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
    @Mock
    private NotificacionService notificacionService;

    private EliminarActividadService service;
    private UUID actividadId;
    private UUID instructorId;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        service = new EliminarActividadService(
                actividadRepository, claseRepository, inscripcionRepository, pagoRepository, paymentGateway, auditService,
                notificacionService);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        actividadId = UUID.randomUUID();
        actividad = new Actividad();
        actividad.setNombre("Running");
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", actividadId);
    }

    @Test
    void eliminar_dueño_marcaBorrado() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(any(), any())).thenReturn(List.of());

        service.eliminar(actividadId, instructorId, false);

        assertThat(actividad.isDeleted()).isTrue();
        verify(actividadRepository).save(actividad);
        verify(notificacionService, never()).notificar(eq(instructorId), any(), any(), any());
    }

    @Test
    void eliminar_admin_puedeBorrarActividadDeOtroInstructor() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(any(), any())).thenReturn(List.of());

        service.eliminar(actividadId, UUID.randomUUID(), true);

        assertThat(actividad.isDeleted()).isTrue();
        verify(actividadRepository).save(actividad);
        verify(notificacionService).notificar(eq(instructorId), eq(TipoNotificacion.ACTIVIDAD_ELIMINADA), any(), eq(actividadId));
    }

    @Test
    void eliminar_cascadeaClasesEInscripciones() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        Clase clase = new Clase();
        clase.setActividad(actividad);
        clase.setEstado(EstadoClase.Programada);
        clase.setFechaHora(Instant.parse("2026-08-20T12:00:00Z"));
        UUID claseId = UUID.randomUUID();
        ReflectionTestUtils.setField(clase, "id", claseId);
        when(claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(any(), any())).thenReturn(List.of(clase));

        UUID alumnoId = UUID.randomUUID();
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        Pago pago = new Pago();
        pago.setEstado(EstadoPago.Retenido);
        pago.setMetodo(MetodoPago.MERCADO_PAGO);
        pago.setMonto(new BigDecimal("4500"));
        pago.setReferenciaExterna("ref-1");
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setEstado(EstadoInscripcion.INSCRIPTO);
        inscripcion.setAlumno(alumno);
        inscripcion.setPago(pago);
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA)).thenReturn(List.of(inscripcion));

        service.eliminar(actividadId, instructorId, false);

        assertThat(clase.getEstado()).isEqualTo(EstadoClase.Cancelada);
        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Cancelado);
        verify(paymentGateway).cancelarPago("ref-1");
        verify(notificacionService).notificar(eq(alumnoId), eq(TipoNotificacion.ACTIVIDAD_ELIMINADA), any(), eq(actividadId));
    }

    @Test
    void eliminar_noTocaClasesYaTerminales() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(any(), any())).thenReturn(List.of());

        service.eliminar(actividadId, instructorId, false);

        verify(inscripcionRepository, never()).findByClaseIdAndEstadoNot(any(), any());
        verify(claseRepository, never()).save(any());
    }

    @Test
    void eliminar_noDueño_lanzaSinPermiso() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        assertThatThrownBy(() -> service.eliminar(actividadId, UUID.randomUUID(), false))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void eliminar_inexistente_lanzaNoEncontrado() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(actividadId, instructorId, false))
                .isInstanceOf(NoEncontradoException.class);
    }
}
