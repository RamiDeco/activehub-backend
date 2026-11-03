package com.activehub.usecases.desbloquearinstructorpenalizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.penalizacion.ComprobantePenalizacion;
import com.activehub.domain.penalizacion.ComprobantePenalizacionRepository;
import com.activehub.domain.penalizacion.EstadoComprobantePenalizacion;
import com.activehub.domain.penalizacion.EstadoPagoPenalizacion;
import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.security.PenalizacionVigenteGuard;
import com.activehub.shared.time.Zonas;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * V36, "Desbloquear instructor": el administrador aprueba el comprobante. La multa queda
 * pagada; si además hay una suspensión vigente, sigue suspendido: pagar no la acorta.
 */
@ExtendWith(MockitoExtension.class)
class DesbloquearInstructorPenalizacionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");

    @Mock private PenalizacionRepository penalizacionRepository;
    @Mock private ComprobantePenalizacionRepository comprobantePenalizacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PenalizacionVigenteGuard penalizacionVigenteGuard;
    @Mock private NotificacionService notificacionService;
    @Mock private AuditService auditService;

    private DesbloquearInstructorPenalizacionService service;
    private UUID instructorId;
    private UUID adminId;
    private Penalizacion multa;
    private ComprobantePenalizacion comprobante;

    @BeforeEach
    void setUp() {
        service = new DesbloquearInstructorPenalizacionService(
                penalizacionRepository, comprobantePenalizacionRepository, usuarioRepository,
                penalizacionVigenteGuard, notificacionService, auditService, Clock.fixed(AHORA, Zonas.AR));

        instructorId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        multa = new Penalizacion();
        multa.setUsuario(instructor);
        multa.setTipo(TipoPenalizacion.ECONOMICA);
        multa.setMonto(new BigDecimal("5000"));
        multa.setEstadoPago(EstadoPagoPenalizacion.EN_REVISION);
        ReflectionTestUtils.setField(multa, "id", UUID.randomUUID());

        comprobante = new ComprobantePenalizacion();
        comprobante.setPenalizacion(multa);
        comprobante.setEstado(EstadoComprobantePenalizacion.EN_REVISION);
    }

    private void conComprobanteEnRevision() {
        when(penalizacionRepository.findById(multa.getId())).thenReturn(Optional.of(multa));
        when(comprobantePenalizacionRepository.findFirstByPenalizacionIdAndEstadoOrderByCreatedAtDesc(
                multa.getId(), EstadoComprobantePenalizacion.EN_REVISION)).thenReturn(Optional.of(comprobante));
        when(usuarioRepository.getReferenceById(adminId)).thenReturn(new Usuario());
    }

    @Test
    void desbloquear_apruebaElComprobanteYLaMultaQuedaPagada() {
        conComprobanteEnRevision();
        when(penalizacionVigenteGuard.suspensionVigente(instructorId)).thenReturn(Optional.empty());

        DesbloquearInstructorPenalizacionResponse response = service.desbloquear(multa.getId(), adminId);

        assertThat(response.estadoPago()).isEqualTo("PAGADA");
        assertThat(response.suspendidoHasta()).isNull();
        assertThat(multa.getEstadoPago()).isEqualTo(EstadoPagoPenalizacion.PAGADA);
        assertThat(multa.getPagoAprobadoAt()).isEqualTo(AHORA);
        assertThat(comprobante.getEstado()).isEqualTo(EstadoComprobantePenalizacion.APROBADO);
        assertThat(comprobante.getRevisadoAt()).isEqualTo(AHORA);
        verify(auditService).registrar(eq(adminId), eq(AuditAccion.MULTA_PAGADA), eq("Penalizacion"), eq(multa.getId()), any());
        verify(notificacionService).notificar(eq(instructorId), eq(TipoNotificacion.MULTA_PAGADA),
                argThat(m -> m.contains("volver a operar")), eq(multa.getId()), eq(Destino.penalizacion(multa.getId())));
    }

    @Test
    void desbloquear_conSuspensionVigente_avisaQueSigueSuspendido() {
        conComprobanteEnRevision();
        Penalizacion suspension = new Penalizacion();
        suspension.setFechaFin(LocalDate.of(2026, 11, 30));
        when(penalizacionVigenteGuard.suspensionVigente(instructorId)).thenReturn(Optional.of(suspension));

        DesbloquearInstructorPenalizacionResponse response = service.desbloquear(multa.getId(), adminId);

        assertThat(response.suspendidoHasta()).isEqualTo(LocalDate.of(2026, 11, 30));
        verify(notificacionService).notificar(eq(instructorId), eq(TipoNotificacion.MULTA_PAGADA),
                argThat(m -> m.contains("suspensión sigue vigente hasta el 2026-11-30")), any(), any());
    }

    /** Sin comprobante enviado no hay nada que verificar: el botón es para aprobar un pago. */
    @Test
    void desbloquear_multaSinComprobanteEnRevision_lanzaValidacion() {
        multa.setEstadoPago(EstadoPagoPenalizacion.PENDIENTE_PAGO);
        when(penalizacionRepository.findById(multa.getId())).thenReturn(Optional.of(multa));

        assertThatThrownBy(() -> service.desbloquear(multa.getId(), adminId))
                .isInstanceOf(ValidacionException.class);
        verify(penalizacionRepository, never()).save(any());
    }
}
