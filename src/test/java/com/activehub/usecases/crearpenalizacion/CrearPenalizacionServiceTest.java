package com.activehub.usecases.crearpenalizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.payments.PaymentGateway;
import com.activehub.shared.security.PermisosService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
    @Mock private PermisosService permisosService;
    @Mock private ClaseRepository claseRepository;
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private PaymentGateway paymentGateway;

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

    /**
     * Solo se penaliza a quien dicta clases (E4Ad-HU06 / RN-13) y eso se decide por el
     * permiso, no por el nombre del rol (RN-19). Por defecto el penalizado es instructor.
     */
    private void usuarioExiste() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(permisosService.puede(usuarioId, "clases.gestionar")).thenReturn(true);
    }

    private void guardaDevolviendoLaMisma() {
        when(penalizacionRepository.saveAndFlush(any(Penalizacion.class))).thenAnswer(inv -> {
            Penalizacion p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });
    }

    @Test
    void crear_usuarioQueNoDaClases_lanzaValidacion() {
        // Un alumno o un administrador no se penalizan: la sancion existe por la inasistencia
        // del profesor. Hasta esta guarda el formulario los ofrecia igual.
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(permisosService.puede(usuarioId, "clases.gestionar")).thenReturn(false);

        assertThatThrownBy(() -> service.crear(
                new CrearPenalizacionRequest(
                        usuarioId, List.of("Económica"), "Motivo", BigDecimal.TEN, null, null),
                adminId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("Solo se puede penalizar a un instructor");
        verify(penalizacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void crear_economicaConMonto_guardaEIncrementaContador() {
        usuarioExiste();
        guardaDevolviendoLaMisma();
        var request = new CrearPenalizacionRequest(
                usuarioId, List.of("Económica"), "No se presentó", new BigDecimal("5000"), null, null);

        CrearPenalizacionResponse response = service.crear(request, adminId);

        assertThat(response.penalizaciones()).hasSize(1);
        assertThat(response.penalizaciones().get(0).monto()).isEqualByComparingTo("5000");
        assertThat(response.cantidadPenalizacionesUsuario()).isEqualTo(1);
        // Una económica no suspende al usuario.
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void crear_economicaSinMonto_lanzaValidacion() {
        usuarioExiste();
        var request = new CrearPenalizacionRequest(usuarioId, List.of("Económica"), "No se presentó", null, null, null);

        assertThatThrownBy(() -> service.crear(request, adminId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("monto");

        verify(penalizacionRepository, never()).saveAndFlush(any());
    }

    /**
     * Decision del usuario: <b>el penalizado sigue pudiendo iniciar sesion</b>. Tiene que poder
     * ver su sancion, sus clases canceladas y sus datos. Antes esto ponia la cuenta en
     * SUSPENDIDO, que es justo la condicion con la que el login rechaza, asi que la suspension
     * lo dejaba afuera de la plataforma. Lo que no puede es operar, y de eso se encarga
     * {@code PenalizacionVigenteGuard} mirando la vigencia.
     */
    @Test
    void crear_suspensionConVigencia_noTocaElEstadoDeLaCuenta() {
        usuarioExiste();
        guardaDevolviendoLaMisma();
        var request = new CrearPenalizacionRequest(
                usuarioId, List.of("Suspensión temporal"), "Reiteradas cancelaciones", null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 20));

        CrearPenalizacionResponse response = service.crear(request, adminId);

        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(response.penalizaciones().get(0).fechaFin()).isEqualTo(LocalDate.of(2026, 6, 20));
    }

    /**
     * Un instructor suspendido no puede dictar, asi que sus clases del periodo se cancelan con
     * la cascada completa: inscripcion cancelada, pago reintegrado y aviso al alumno. Sin esto
     * la gente quedaba anotada y pagando por una clase que no iba a existir.
     */
    @Test
    void crear_suspension_cancelaLasClasesDelPeriodoYReintegra() {
        usuarioExiste();
        guardaDevolviendoLaMisma();

        var actividad = new com.activehub.domain.actividad.Actividad();
        actividad.setNombre("Yoga");
        var clase = new com.activehub.domain.actividad.Clase();
        clase.setActividad(actividad);
        clase.setFechaHora(java.time.Instant.parse("2026-06-10T15:00:00Z"));
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());

        var alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", UUID.randomUUID());
        var pago = new com.activehub.domain.inscripcion.Pago();
        pago.setEstado(com.activehub.domain.inscripcion.EstadoPago.Retenido);
        pago.setReferenciaExterna("ref-1");
        var inscripcion = new com.activehub.domain.inscripcion.Inscripcion();
        inscripcion.setAlumno(alumno);
        inscripcion.setPago(pago);
        inscripcion.setEstado(com.activehub.domain.inscripcion.EstadoInscripcion.INSCRIPTO);

        when(claseRepository.findVivasDeInstructorEntre(
                org.mockito.ArgumentMatchers.eq(usuarioId), any(), any(), any()))
                .thenReturn(List.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(
                clase.getId(), com.activehub.domain.inscripcion.EstadoInscripcion.CANCELADA))
                .thenReturn(List.of(inscripcion));

        service.crear(new CrearPenalizacionRequest(
                usuarioId, List.of("Suspensión temporal"), "Reiteradas cancelaciones", null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 20)), adminId);

        assertThat(clase.getEstado()).isEqualTo(com.activehub.domain.actividad.EstadoClase.Cancelada);
        assertThat(inscripcion.getEstado())
                .isEqualTo(com.activehub.domain.inscripcion.EstadoInscripcion.CANCELADA);
        assertThat(pago.getEstado()).isEqualTo(com.activehub.domain.inscripcion.EstadoPago.Cancelado);
        verify(paymentGateway).cancelarPago("ref-1");
        verify(notificacionService).notificar(
                org.mockito.ArgumentMatchers.eq(alumno.getId()),
                org.mockito.ArgumentMatchers.eq(com.activehub.shared.notificacion.TipoNotificacion.CLASE_CANCELADA),
                any(), org.mockito.ArgumentMatchers.eq(clase.getId()));
    }

    @Test
    void crear_suspensionSinFechas_lanzaValidacion() {
        usuarioExiste();
        var request = new CrearPenalizacionRequest(
                usuarioId, List.of("Suspensión temporal"), "Reiteradas cancelaciones", null, null, null);

        assertThatThrownBy(() -> service.crear(request, adminId))
                .isInstanceOf(ValidacionException.class);

        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void crear_suspensionConFinAnteriorAInicio_lanzaValidacion() {
        usuarioExiste();
        var request = new CrearPenalizacionRequest(
                usuarioId, List.of("Suspensión temporal"), "Motivo", null,
                LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 1));

        assertThatThrownBy(() -> service.crear(request, adminId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void crear_aSiMismo_lanzaValidacion() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        var request = new CrearPenalizacionRequest(
                usuarioId, List.of("Económica"), "Motivo", new BigDecimal("100"), null, null);

        assertThatThrownBy(() -> service.crear(request, usuarioId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void crear_tipoEconomica_seMapeaAlEnum() {
        usuarioExiste();
        guardaDevolviendoLaMisma();
        var request = new CrearPenalizacionRequest(
                usuarioId, List.of("Económica"), "Motivo", new BigDecimal("100"), null, null);

        service.crear(request, adminId);

        verify(penalizacionRepository).saveAndFlush(
                org.mockito.ArgumentMatchers.argThat(p -> p.getTipo() == TipoPenalizacion.ECONOMICA));
    }

    @Test
    void crear_ambosTipos_guardaDosPenalizaciones() {
        // Decisión del usuario: multa + suspensión se guardan como dos filas, no como una
        // fila "mixta". El enum de la base sigue teniendo dos valores.
        usuarioExiste();
        guardaDevolviendoLaMisma();
        var request = new CrearPenalizacionRequest(
                usuarioId, List.of("Económica", "Suspensión temporal"), "Ausencias reiteradas",
                new BigDecimal("8000"), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 20));

        CrearPenalizacionResponse response = service.crear(request, adminId);

        assertThat(response.penalizaciones()).hasSize(2);
        assertThat(response.penalizaciones()).extracting(CrearPenalizacionResponse.Aplicada::tipo)
                .containsExactly("Económica", "Suspensión temporal");
        // El contador del usuario suma las dos.
        assertThat(response.cantidadPenalizacionesUsuario()).isEqualTo(2);
        // La cuenta sigue ACTIVA: la suspension corta la operacion, no la sesion.
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        verify(penalizacionRepository, org.mockito.Mockito.times(2)).saveAndFlush(any(Penalizacion.class));
    }

    @Test
    void crear_suspensionMasCortaQueElMinimo_lanzaValidacion() {
        usuarioExiste();
        var request = new CrearPenalizacionRequest(
                usuarioId, List.of("Suspensión temporal"), "Motivo", null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10));

        assertThatThrownBy(() -> service.crear(request, adminId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("15 días");

        verify(penalizacionRepository, never()).saveAndFlush(any());
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void crear_tipoRepetido_noDuplicaLaSancion() {
        usuarioExiste();
        guardaDevolviendoLaMisma();
        var request = new CrearPenalizacionRequest(
                usuarioId, List.of("Económica", "Económica"), "Motivo", new BigDecimal("100"), null, null);

        CrearPenalizacionResponse response = service.crear(request, adminId);

        assertThat(response.penalizaciones()).hasSize(1);
    }
}
