package com.activehub.usecases.inscribirse;

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
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.InscripcionYaExisteException;
import com.activehub.shared.error.SinCuposDisponiblesException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
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
class InscribirseServiceTest {

    private static final Instant AHORA = Instant.parse("2026-08-01T00:00:00Z");

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private AuditService auditService;
    @Mock
    private NotificacionService notificacionService;

    private InscribirseService service;
    private UUID claseId;
    private UUID alumnoId;
    private UUID instructorId;
    private Clase clase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        service = new InscribirseService(
                claseRepository, inscripcionRepository, pagoRepository, usuarioRepository, paymentGateway, auditService,
                notificacionService, clock);

        claseId = UUID.randomUUID();
        alumnoId = UUID.randomUUID();
        instructorId = UUID.randomUUID();

        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setPrecio(new BigDecimal("4500"));
        actividad.setNombre("Running");
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        clase = new Clase();
        clase.setEstado(EstadoClase.Programada);
        clase.setFechaHora(AHORA.plus(Duration.ofDays(2)));
        clase.setActividad(actividad);
        ReflectionTestUtils.setField(clase, "id", claseId);
    }

    private void stubGuardados() {
        when(inscripcionRepository.saveAndFlush(any(Inscripcion.class))).thenAnswer(inv -> {
            Inscripcion i = inv.getArgument(0);
            if (i.getId() == null) {
                ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
            }
            ReflectionTestUtils.setField(i, "createdAt", AHORA);
            return i;
        });
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });
    }

    @Test
    void inscribirse_mercadoPago_creaInscriptoYPagoRetenido() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.empty());
        when(claseRepository.ocuparCupo(claseId)).thenReturn(1);
        when(usuarioRepository.getReferenceById(alumnoId)).thenReturn(new Usuario());
        when(paymentGateway.iniciarPago(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(new PaymentGateway.IniciarPagoResultado("ref-123", "https://mock/checkout"));
        stubGuardados();

        InscribirseResponse response = service.inscribirse(claseId, new InscribirseRequest("Mercado Pago"), alumnoId);

        assertThat(response.estado()).isEqualTo("Inscripto");
        assertThat(response.pagoId()).isNotNull();
        verify(claseRepository).ocuparCupo(claseId);
        verify(paymentGateway).iniciarPago(any(), org.mockito.ArgumentMatchers.eq(450000L));
        verify(notificacionService).notificar(eq(alumnoId), eq(TipoNotificacion.INSCRIPCION_CONFIRMADA), any(), any());
        verify(notificacionService).notificar(eq(instructorId), eq(TipoNotificacion.NUEVA_INSCRIPCION), any(), any());
    }

    @Test
    void inscribirse_efectivo_creaPagoPendienteYPagoEfectivo_sinLlamarGateway() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.empty());
        when(claseRepository.ocuparCupo(claseId)).thenReturn(1);
        when(usuarioRepository.getReferenceById(alumnoId)).thenReturn(new Usuario());
        stubGuardados();

        InscribirseResponse response = service.inscribirse(claseId, new InscribirseRequest("Efectivo"), alumnoId);

        assertThat(response.estado()).isEqualTo("PagoPendiente");
        verify(paymentGateway, never()).iniciarPago(any(), org.mockito.ArgumentMatchers.anyLong());
        verify(notificacionService).notificar(eq(alumnoId), eq(TipoNotificacion.INSCRIPCION_CONFIRMADA), any(), any());
    }

    @Test
    void inscribirse_conPreinscripcionExistente_reutilizaLaMismaFila() {
        Inscripcion existente = new Inscripcion();
        existente.setEstado(EstadoInscripcion.PRE_INSCRIPCION);
        UUID inscripcionId = UUID.randomUUID();
        ReflectionTestUtils.setField(existente, "id", inscripcionId);
        ReflectionTestUtils.setField(existente, "createdAt", AHORA.minus(Duration.ofDays(6)));

        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.of(existente));
        when(claseRepository.ocuparCupo(claseId)).thenReturn(1);
        stubGuardados();
        when(paymentGateway.iniciarPago(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(new PaymentGateway.IniciarPagoResultado("ref-123", "https://mock/checkout"));

        InscribirseResponse response = service.inscribirse(claseId, new InscribirseRequest("Mercado Pago"), alumnoId);

        assertThat(response.id()).isEqualTo(inscripcionId);
        verify(inscripcionRepository).saveAndFlush(eq(existente));
        verify(usuarioRepository, never()).getReferenceById(any());
    }

    @Test
    void inscribirse_masDe4Dias_lanzaValidacion() {
        clase.setFechaHora(AHORA.plus(Duration.ofDays(10)));
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.inscribirse(claseId, new InscribirseRequest("Efectivo"), alumnoId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void inscribirse_menosDe1Hora_lanzaValidacion() {
        clase.setFechaHora(AHORA.plus(Duration.ofMinutes(30)));
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.inscribirse(claseId, new InscribirseRequest("Efectivo"), alumnoId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void inscribirse_sinCupos_lanzaSinCuposDisponibles() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.empty());
        when(claseRepository.ocuparCupo(claseId)).thenReturn(0);

        assertThatThrownBy(() -> service.inscribirse(claseId, new InscribirseRequest("Efectivo"), alumnoId))
                .isInstanceOf(SinCuposDisponiblesException.class);

        verify(inscripcionRepository, never()).saveAndFlush(any());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void inscribirse_yaTieneInscripcionDefinitivaActiva_lanzaYaExiste() {
        Inscripcion existente = new Inscripcion();
        existente.setEstado(EstadoInscripcion.INSCRIPTO);

        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.inscribirse(claseId, new InscribirseRequest("Efectivo"), alumnoId))
                .isInstanceOf(InscripcionYaExisteException.class);

        verify(claseRepository, never()).ocuparCupo(any());
    }
}
