package com.activehub.usecases.levantarsuspension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
 * V36: el administrador levanta una suspensión antes de tiempo, con motivo. No deshace lo que
 * la suspensión ya hizo ni libera una multa que la acompañe.
 */
@ExtendWith(MockitoExtension.class)
class LevantarSuspensionServiceTest {

    /** Mediodía del 2 de noviembre de 2026 en Argentina. */
    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");

    @Mock private PenalizacionRepository penalizacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PenalizacionVigenteGuard penalizacionVigenteGuard;
    @Mock private NotificacionService notificacionService;
    @Mock private AuditService auditService;

    private LevantarSuspensionService service;
    private UUID instructorId;
    private UUID adminId;
    private Penalizacion suspension;

    @BeforeEach
    void setUp() {
        service = new LevantarSuspensionService(
                penalizacionRepository, usuarioRepository, penalizacionVigenteGuard,
                notificacionService, auditService, Clock.fixed(AHORA, Zonas.AR));
        instructorId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        suspension = new Penalizacion();
        suspension.setUsuario(instructor);
        suspension.setTipo(TipoPenalizacion.SUSPENSION_TEMPORAL);
        suspension.setFechaInicio(LocalDate.of(2026, 10, 20));
        suspension.setFechaFin(LocalDate.of(2026, 11, 20));
        ReflectionTestUtils.setField(suspension, "id", UUID.randomUUID());
    }

    private LevantarSuspensionRequest motivo(String texto) {
        return new LevantarSuspensionRequest(texto);
    }

    @Test
    void levantar_suspensionVigente_quedaLevantadaConSuMotivoYSeAvisa() {
        when(penalizacionRepository.findById(suspension.getId())).thenReturn(Optional.of(suspension));
        when(usuarioRepository.getReferenceById(adminId)).thenReturn(new Usuario());
        when(penalizacionVigenteGuard.tieneMultaPendiente(instructorId)).thenReturn(false);
        when(penalizacionVigenteGuard.suspensionVigente(instructorId)).thenReturn(Optional.empty());

        LevantarSuspensionResponse response = service.levantar(suspension.getId(), motivo("Se aclaró la situación"), adminId);

        assertThat(response.levantadaAt()).isEqualTo(AHORA);
        assertThat(response.multaPendiente()).isFalse();
        assertThat(suspension.getMotivoLevantamiento()).isEqualTo("Se aclaró la situación");
        verify(auditService).registrar(eq(adminId), eq(AuditAccion.SUSPENSION_LEVANTADA), eq("Penalizacion"),
                eq(suspension.getId()), eq("ADMIN · Se aclaró la situación"));
        verify(notificacionService).notificar(eq(instructorId), eq(TipoNotificacion.SUSPENSION_LEVANTADA),
                argThat(m -> m.contains("volver a operar")), eq(suspension.getId()),
                eq(Destino.penalizacion(suspension.getId())));
    }

    /** Si además tenía una multa, sigue bloqueado: esa se libera aprobando el comprobante. */
    @Test
    void levantar_conMultaSinPagar_avisaQueTodaviaTieneQuePagarla() {
        when(penalizacionRepository.findById(suspension.getId())).thenReturn(Optional.of(suspension));
        when(usuarioRepository.getReferenceById(adminId)).thenReturn(new Usuario());
        when(penalizacionVigenteGuard.tieneMultaPendiente(instructorId)).thenReturn(true);
        when(penalizacionVigenteGuard.suspensionVigente(instructorId)).thenReturn(Optional.empty());

        LevantarSuspensionResponse response = service.levantar(suspension.getId(), motivo("Error de carga"), adminId);

        assertThat(response.multaPendiente()).isTrue();
        verify(notificacionService).notificar(eq(instructorId), eq(TipoNotificacion.SUSPENSION_LEVANTADA),
                argThat(m -> m.contains("multa pendiente de pago")), any(), any());
    }

    @Test
    void levantar_sinMotivo_lanzaValidacion() {
        assertThatThrownBy(() -> service.levantar(suspension.getId(), motivo(""), adminId))
                .isInstanceOf(ValidacionException.class);
        verify(penalizacionRepository, never()).save(any());
    }

    @Test
    void levantar_yaLevantada_lanzaValidacion() {
        suspension.setLevantadaAt(Instant.parse("2026-10-25T12:00:00Z"));
        when(penalizacionRepository.findById(suspension.getId())).thenReturn(Optional.of(suspension));

        assertThatThrownBy(() -> service.levantar(suspension.getId(), motivo("Otra vez"), adminId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("ya fue levantada");
    }

    @Test
    void levantar_suspensionQueYaTermino_lanzaValidacion() {
        suspension.setFechaFin(LocalDate.of(2026, 11, 1));
        when(penalizacionRepository.findById(suspension.getId())).thenReturn(Optional.of(suspension));

        assertThatThrownBy(() -> service.levantar(suspension.getId(), motivo("Tarde"), adminId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("ya terminó");
    }

    @Test
    void levantar_unaMulta_lanzaValidacion() {
        suspension.setTipo(TipoPenalizacion.ECONOMICA);
        when(penalizacionRepository.findById(suspension.getId())).thenReturn(Optional.of(suspension));

        assertThatThrownBy(() -> service.levantar(suspension.getId(), motivo("No corresponde"), adminId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("suspensión temporal");
    }
}
