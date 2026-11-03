package com.activehub.usecases.invalidarcomprobantepenalizacion;

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
import com.activehub.shared.time.Zonas;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** V36, "Notificar invalidez": el comprobante no sirve y el instructor puede mandar otro. */
@ExtendWith(MockitoExtension.class)
class InvalidarComprobantePenalizacionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");

    @Mock private PenalizacionRepository penalizacionRepository;
    @Mock private ComprobantePenalizacionRepository comprobantePenalizacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private AuditService auditService;

    private InvalidarComprobantePenalizacionService service;
    private UUID instructorId;
    private UUID adminId;
    private Penalizacion multa;
    private ComprobantePenalizacion comprobante;

    @BeforeEach
    void setUp() {
        service = new InvalidarComprobantePenalizacionService(
                penalizacionRepository, comprobantePenalizacionRepository, usuarioRepository,
                notificacionService, auditService, Clock.fixed(AHORA, Zonas.AR));
        instructorId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        multa = new Penalizacion();
        multa.setUsuario(instructor);
        multa.setTipo(TipoPenalizacion.ECONOMICA);
        multa.setEstadoPago(EstadoPagoPenalizacion.EN_REVISION);
        ReflectionTestUtils.setField(multa, "id", UUID.randomUUID());

        comprobante = new ComprobantePenalizacion();
        comprobante.setPenalizacion(multa);
        comprobante.setEstado(EstadoComprobantePenalizacion.EN_REVISION);
    }

    @Test
    void invalidar_marcaElComprobanteConSuMotivoYLaMultaVuelveAPendiente() {
        when(penalizacionRepository.findById(multa.getId())).thenReturn(Optional.of(multa));
        when(comprobantePenalizacionRepository.findFirstByPenalizacionIdAndEstadoOrderByCreatedAtDesc(
                multa.getId(), EstadoComprobantePenalizacion.EN_REVISION)).thenReturn(Optional.of(comprobante));
        when(usuarioRepository.getReferenceById(adminId)).thenReturn(new Usuario());

        InvalidarComprobantePenalizacionResponse response = service.invalidar(
                multa.getId(), new InvalidarComprobantePenalizacionRequest("El monto no coincide"), adminId);

        assertThat(response.estadoPago()).isEqualTo("PENDIENTE_PAGO");
        assertThat(multa.getEstadoPago()).isEqualTo(EstadoPagoPenalizacion.PENDIENTE_PAGO);
        assertThat(comprobante.getEstado()).isEqualTo(EstadoComprobantePenalizacion.INVALIDO);
        assertThat(comprobante.getMotivoInvalidez()).isEqualTo("El monto no coincide");
        assertThat(comprobante.getRevisadoAt()).isEqualTo(AHORA);
        verify(auditService).registrar(eq(adminId), eq(AuditAccion.COMPROBANTE_PENALIZACION_INVALIDO),
                eq("Penalizacion"), eq(multa.getId()), eq("INVALIDO · El monto no coincide"));
        verify(notificacionService).notificar(eq(instructorId), eq(TipoNotificacion.COMPROBANTE_PENALIZACION_INVALIDO),
                argThat(m -> m.contains("El monto no coincide")), eq(multa.getId()), eq(Destino.penalizacion(multa.getId())));
    }

    @Test
    void invalidar_sinMotivo_lanzaValidacion() {
        assertThatThrownBy(() -> service.invalidar(
                multa.getId(), new InvalidarComprobantePenalizacionRequest("   "), adminId))
                .isInstanceOf(ValidacionException.class);
        verify(penalizacionRepository, never()).save(any());
    }

    @Test
    void invalidar_multaSinComprobanteEnRevision_lanzaValidacion() {
        multa.setEstadoPago(EstadoPagoPenalizacion.PAGADA);
        when(penalizacionRepository.findById(multa.getId())).thenReturn(Optional.of(multa));

        assertThatThrownBy(() -> service.invalidar(
                multa.getId(), new InvalidarComprobantePenalizacionRequest("Ilegible"), adminId))
                .isInstanceOf(ValidacionException.class);
    }
}
