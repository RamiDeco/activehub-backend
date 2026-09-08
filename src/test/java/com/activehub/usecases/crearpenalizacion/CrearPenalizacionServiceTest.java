package com.activehub.usecases.crearpenalizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.NotificacionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CrearPenalizacionServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PenalizacionRepository penalizacionRepository;
    @Mock private AuditService auditService;
    @Mock private NotificacionService notificacionService;

    @InjectMocks private CrearPenalizacionService service;

    private UUID usuarioId;
    private UUID adminId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        usuario = new Usuario();
        usuario.setNombre("Laura");
        usuario.setApellido("Gimenez");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
    }

    private void usuarioExiste() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
    }

    private void guardaDevolviendoLaMisma() {
        when(penalizacionRepository.saveAndFlush(any(Penalizacion.class))).thenAnswer(inv -> {
            Penalizacion p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });
    }

    @Test
    void crear_economicaConMonto_guardaEIncrementaContador() {
        usuarioExiste();
        guardaDevolviendoLaMisma();
        var request = new CrearPenalizacionRequest(
                usuarioId, "Económica", "No se presentó", new BigDecimal("5000"), null, null);

        CrearPenalizacionResponse response = service.crear(request, adminId);

        assertThat(response.monto()).isEqualByComparingTo("5000");
        assertThat(response.cantidadPenalizacionesUsuario()).isEqualTo(1);
        // Una económica no suspende al usuario.
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void crear_economicaSinMonto_lanzaValidacion() {
        usuarioExiste();
        var request = new CrearPenalizacionRequest(usuarioId, "Económica", "No se presentó", null, null, null);

        assertThatThrownBy(() -> service.crear(request, adminId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("monto");

        verify(penalizacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void crear_suspensionConVigencia_dejaAlUsuarioSuspendido() {
        usuarioExiste();
        guardaDevolviendoLaMisma();
        var request = new CrearPenalizacionRequest(
                usuarioId, "Suspensión temporal", "Reiteradas cancelaciones", null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15));

        CrearPenalizacionResponse response = service.crear(request, adminId);

        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.SUSPENDIDO);
        assertThat(response.fechaFin()).isEqualTo(LocalDate.of(2026, 6, 15));
    }

    @Test
    void crear_suspensionSinFechas_lanzaValidacion() {
        usuarioExiste();
        var request = new CrearPenalizacionRequest(
                usuarioId, "Suspensión temporal", "Reiteradas cancelaciones", null, null, null);

        assertThatThrownBy(() -> service.crear(request, adminId))
                .isInstanceOf(ValidacionException.class);

        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void crear_suspensionConFinAnteriorAInicio_lanzaValidacion() {
        usuarioExiste();
        var request = new CrearPenalizacionRequest(
                usuarioId, "Suspensión temporal", "Motivo", null,
                LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 1));

        assertThatThrownBy(() -> service.crear(request, adminId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void crear_aSiMismo_lanzaValidacion() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        var request = new CrearPenalizacionRequest(
                usuarioId, "Económica", "Motivo", new BigDecimal("100"), null, null);

        assertThatThrownBy(() -> service.crear(request, usuarioId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void crear_tipoEconomica_seMapeaAlEnum() {
        usuarioExiste();
        guardaDevolviendoLaMisma();
        var request = new CrearPenalizacionRequest(
                usuarioId, "Económica", "Motivo", new BigDecimal("100"), null, null);

        service.crear(request, adminId);

        verify(penalizacionRepository).saveAndFlush(
                org.mockito.ArgumentMatchers.argThat(p -> p.getTipo() == TipoPenalizacion.ECONOMICA));
    }
}
