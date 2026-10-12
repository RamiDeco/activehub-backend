package com.activehub.usecases.levantarsuspensionesvencidas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.time.Zonas;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * E4Ad-HU06 criterio 4: la suspensión temporal tiene que terminar sola. Ojo con el "hoy":
 * se calcula en zona Argentina ({@link Zonas#AR}), no en la del servidor — un job corriendo
 * a las 02:00 UTC está todavía en el día anterior para el negocio.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LevantarSuspensionesVencidasServiceTest {

    // 03:00 UTC del 20/06 = 00:00 del 20/06 en Argentina.
    private static final Instant AHORA = Instant.parse("2026-06-20T03:00:00Z");
    private static final LocalDate HOY_AR = LocalDate.of(2026, 6, 20);

    @Mock private PenalizacionRepository penalizacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditService auditService;

    private LevantarSuspensionesVencidasService service;

    @BeforeEach
    void setUp() {
        service = new LevantarSuspensionesVencidasService(
                penalizacionRepository, usuarioRepository, auditService, Clock.fixed(AHORA, Zonas.AR));
    }

    private Usuario usuario(EstadoUsuario estado) {
        Usuario u = new Usuario();
        u.setEstado(estado);
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        return u;
    }

    private Penalizacion suspension(Usuario usuario, LocalDate fechaFin) {
        Penalizacion p = new Penalizacion();
        p.setUsuario(usuario);
        p.setTipo(TipoPenalizacion.SUSPENSION_TEMPORAL);
        p.setFechaInicio(fechaFin.minusDays(7));
        p.setFechaFin(fechaFin);
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        return p;
    }

    @Test
    void levantar_suspensionVencida_devuelveElUsuarioAActivoYAudita() {
        Usuario suspendido = usuario(EstadoUsuario.SUSPENDIDO);
        Penalizacion vencida = suspension(suspendido, HOY_AR.minusDays(1));

        when(penalizacionRepository.findSuspensionesVencidas(TipoPenalizacion.SUSPENSION_TEMPORAL, HOY_AR))
                .thenReturn(List.of(vencida));
        when(penalizacionRepository.findAllConDetalle()).thenReturn(List.of(vencida));

        assertThat(service.levantar()).isEqualTo(1);
        assertThat(suspendido.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        verify(usuarioRepository).save(suspendido);
        // actorId null = lo hizo el sistema, no un admin.
        verify(auditService).registrar(
                isNull(), eq(AuditAccion.SUSPENSION_LEVANTADA), eq("Usuario"), eq(suspendido.getId()), any());
    }

    @Test
    void levantar_conOtraSuspensionTodaviaVigente_noReactiva() {
        // Sanciones superpuestas: la más larga tiene que seguir valiendo.
        Usuario suspendido = usuario(EstadoUsuario.SUSPENDIDO);
        Penalizacion vencida = suspension(suspendido, HOY_AR.minusDays(2));
        Penalizacion vigente = suspension(suspendido, HOY_AR.plusDays(10));

        when(penalizacionRepository.findSuspensionesVencidas(TipoPenalizacion.SUSPENSION_TEMPORAL, HOY_AR))
                .thenReturn(List.of(vencida));
        when(penalizacionRepository.findAllConDetalle()).thenReturn(List.of(vencida, vigente));

        assertThat(service.levantar()).isZero();
        assertThat(suspendido.getEstado()).isEqualTo(EstadoUsuario.SUSPENDIDO);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void levantar_suspensionQueVenceHoy_noReactivaTodavia() {
        // "Vence hoy" = todavía vigente: el usuario recupera el acceso recién mañana.
        Usuario suspendido = usuario(EstadoUsuario.SUSPENDIDO);
        Penalizacion venceHoy = suspension(suspendido, HOY_AR);

        when(penalizacionRepository.findSuspensionesVencidas(TipoPenalizacion.SUSPENSION_TEMPORAL, HOY_AR))
                .thenReturn(List.of(venceHoy));
        when(penalizacionRepository.findAllConDetalle()).thenReturn(List.of(venceHoy));

        assertThat(service.levantar()).isZero();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void levantar_usuarioQueYaEstabaActivo_noLoToca() {
        Usuario activo = usuario(EstadoUsuario.ACTIVO);
        Penalizacion vencida = suspension(activo, HOY_AR.minusDays(3));

        when(penalizacionRepository.findSuspensionesVencidas(TipoPenalizacion.SUSPENSION_TEMPORAL, HOY_AR))
                .thenReturn(List.of(vencida));

        assertThat(service.levantar()).isZero();
        verify(usuarioRepository, never()).save(any());
        verify(auditService, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void levantar_sinSuspensionesVencidas_noHaceNada() {
        when(penalizacionRepository.findSuspensionesVencidas(TipoPenalizacion.SUSPENSION_TEMPORAL, HOY_AR))
                .thenReturn(List.of());

        assertThat(service.levantar()).isZero();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void levantar_varios_reactivaSoloALosQueCorresponde() {
        Usuario libre = usuario(EstadoUsuario.SUSPENDIDO);
        Usuario conOtraVigente = usuario(EstadoUsuario.SUSPENDIDO);
        Penalizacion vencidaLibre = suspension(libre, HOY_AR.minusDays(1));
        Penalizacion vencidaOtro = suspension(conOtraVigente, HOY_AR.minusDays(1));
        Penalizacion vigenteOtro = suspension(conOtraVigente, HOY_AR.plusDays(5));

        when(penalizacionRepository.findSuspensionesVencidas(TipoPenalizacion.SUSPENSION_TEMPORAL, HOY_AR))
                .thenReturn(List.of(vencidaLibre, vencidaOtro));
        when(penalizacionRepository.findAllConDetalle())
                .thenReturn(List.of(vencidaLibre, vencidaOtro, vigenteOtro));

        assertThat(service.levantar()).isEqualTo(1);
        assertThat(libre.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(conOtraVigente.getEstado()).isEqualTo(EstadoUsuario.SUSPENDIDO);
    }
}
