package com.activehub.usecases.resolverdenuncia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.denuncia.EstadoDenuncia;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.MetodoPago;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.denuncia.ResolucionDenuncia;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.domain.penalizacion.VentanaPenalizacion;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.payments.PaymentGateway;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ResolverDenunciaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-06-01T12:00:00Z");

    @Mock
    private DenunciaRepository denunciaRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PenalizacionRepository penalizacionRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private AuditService auditService;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private ReseniaRepository reseniaRepository;

    private ResolverDenunciaService service;
    private UUID denunciaId;
    private UUID claseId;
    private UUID alumnoId;
    private UUID instructorId;
    private Usuario instructor;
    private Denuncia denuncia;

    @BeforeEach
    void setUp() {
        service = new ResolverDenunciaService(
                denunciaRepository, inscripcionRepository, pagoRepository, usuarioRepository, penalizacionRepository,
                reseniaRepository, paymentGateway, auditService, notificacionService,
                Clock.fixed(AHORA, ZoneOffset.UTC));

        instructorId = UUID.randomUUID();
        instructor = new Usuario();
        instructor.setCantidadPenalizaciones(0);
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setNombre("Running");
        actividad.setInstructor(instructor);

        claseId = UUID.randomUUID();
        Clase clase = new Clase();
        clase.setActividad(actividad);
        clase.setFechaHora(Instant.parse("2026-08-20T12:00:00Z"));
        ReflectionTestUtils.setField(clase, "id", claseId);

        alumnoId = UUID.randomUUID();
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        denunciaId = UUID.randomUUID();
        denuncia = new Denuncia();
        denuncia.setClase(clase);
        denuncia.setAlumno(alumno);
        denuncia.setMotivo("No se presentó");
        ReflectionTestUtils.setField(denuncia, "id", denunciaId);

        when(denunciaRepository.findById(denunciaId)).thenReturn(Optional.of(denuncia));
    }

    private ResolverDenunciaRequest req(ResolucionDenuncia accion) {
        // SUSPENDER exige plazo: se manda el mínimo de negocio y sin multa.
        Integer dias = accion == ResolucionDenuncia.SUSPENDER ? VentanaPenalizacion.MINIMO_DIAS_SUSPENSION : null;
        return new ResolverDenunciaRequest(accion.name(), null, null, dias);
    }

    @Test
    void resolver_reintegrarConPagoRetenido_cancelaInscripcionYPago() {
        Pago pago = new Pago();
        pago.setEstado(EstadoPago.Retenido);
        pago.setMetodo(MetodoPago.MERCADO_PAGO);
        pago.setMonto(new BigDecimal("4500"));
        pago.setReferenciaExterna("ref-1");
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setEstado(EstadoInscripcion.INSCRIPTO);
        inscripcion.setPago(pago);

        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.of(inscripcion));

        ResolverDenunciaResponse response = service.resolver(denunciaId, req(ResolucionDenuncia.REINTEGRAR), UUID.randomUUID());

        assertThat(response.estado()).isEqualTo("Resuelta");
        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Cancelado);
        verify(paymentGateway).cancelarPago("ref-1");
        verify(notificacionService).notificar(eq(alumnoId), eq(TipoNotificacion.DENUNCIA_RESUELTA), any(), eq(denunciaId));
        verify(notificacionService, never()).notificar(eq(instructorId), any(), any(), any());
    }

    @Test
    void resolver_reintegrarPagoEfectivo_noLlamaGateway() {
        Pago pago = new Pago();
        pago.setEstado(EstadoPago.Efectivo);
        pago.setMetodo(MetodoPago.EFECTIVO);
        pago.setMonto(new BigDecimal("4500"));
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setEstado(EstadoInscripcion.INSCRIPTO);
        inscripcion.setPago(pago);

        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.of(inscripcion));

        service.resolver(denunciaId, req(ResolucionDenuncia.REINTEGRAR), UUID.randomUUID());

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Efectivo);
        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
        verify(paymentGateway, never()).cancelarPago(any());
    }

    /** La suspensión ahora crea una Penalizacion, que necesita id propio para auditarse. */
    private void penalizacionConId() {
        when(penalizacionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            com.activehub.domain.penalizacion.Penalizacion p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });
    }

    @Test
    void resolver_suspender_afectaAlInstructorNoAlAlumno() {
        penalizacionConId();

        ResolverDenunciaResponse response = service.resolver(denunciaId, req(ResolucionDenuncia.SUSPENDER), UUID.randomUUID());

        assertThat(response.estado()).isEqualTo("Resuelta");
        assertThat(instructor.getEstado()).isEqualTo(EstadoUsuario.SUSPENDIDO);
        verify(usuarioRepository).save(instructor);
        verify(notificacionService).notificar(eq(alumnoId), eq(TipoNotificacion.DENUNCIA_RESUELTA), any(), eq(denunciaId));
        verify(notificacionService).notificar(eq(instructorId), eq(TipoNotificacion.INSTRUCTOR_SUSPENDIDO), any(), eq(denunciaId));
    }

    @Test
    void resolver_penalizar_creaPenalizacionYSumaContadorDelInstructor() {
        // saveAndFlush: la penalización necesita id propio para poder auditarla.
        when(penalizacionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            com.activehub.domain.penalizacion.Penalizacion p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });

        service.resolver(denunciaId, req(ResolucionDenuncia.PENALIZAR), UUID.randomUUID());

        assertThat(instructor.getCantidadPenalizaciones()).isEqualTo(1);
        verify(penalizacionRepository).saveAndFlush(any());
        verify(usuarioRepository).save(instructor);
        verify(notificacionService).notificar(eq(alumnoId), eq(TipoNotificacion.DENUNCIA_RESUELTA), any(), eq(denunciaId));
        verify(notificacionService).notificar(eq(instructorId), eq(TipoNotificacion.PENALIZACION_APLICADA), any(), eq(denunciaId));
    }

    @Test
    void resolver_desestimar_soloCierraElCaso() {
        ResolverDenunciaResponse response = service.resolver(denunciaId, req(ResolucionDenuncia.DESESTIMAR), UUID.randomUUID());

        assertThat(response.estado()).isEqualTo("Resuelta");
        assertThat(instructor.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(instructor.getCantidadPenalizaciones()).isEqualTo(0);
        verify(notificacionService).notificar(eq(alumnoId), eq(TipoNotificacion.DENUNCIA_RESUELTA), any(), eq(denunciaId));
        verify(notificacionService).notificar(eq(instructorId), eq(TipoNotificacion.DENUNCIA_DESESTIMADA), any(), eq(denunciaId));
    }

    @Test
    void resolver_yaResuelta_lanzaValidacion() {
        denuncia.setEstado(EstadoDenuncia.RESUELTA);

        assertThatThrownBy(() -> service.resolver(denunciaId, req(ResolucionDenuncia.DESESTIMAR), UUID.randomUUID()))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void resolver_accionInvalida_lanzaValidacion() {
        assertThatThrownBy(() -> service.resolver(denunciaId, new ResolverDenunciaRequest("NO_EXISTE", null, null, null), UUID.randomUUID()))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void resolver_inexistente_lanzaNoEncontrado() {
        when(denunciaRepository.findById(denunciaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolver(denunciaId, req(ResolucionDenuncia.DESESTIMAR), UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void resolver_guardaLaResolucionYElDetalle() {
        // Antes solo se guardaba "Resuelta": el denunciante no tenía forma de saber qué se
        // decidió (E3A-HU11 criterios 2 y 7).
        service.resolver(
                denunciaId, new ResolverDenunciaRequest("DESESTIMAR", "  No encontramos evidencia.  ", null, null), UUID.randomUUID());

        assertThat(denuncia.getResolucion()).isEqualTo(ResolucionDenuncia.DESESTIMAR);
        assertThat(denuncia.getDetalle()).isEqualTo("No encontramos evidencia.");
        verify(denunciaRepository).save(denuncia);
    }

    @Test
    void resolver_detalleVacio_quedaNulo() {
        service.resolver(denunciaId, new ResolverDenunciaRequest("DESESTIMAR", "   ", null, null), UUID.randomUUID());

        assertThat(denuncia.getDetalle()).isNull();
    }

    @Test
    void resolver_ocultarResenia_sobreDenunciaDeClase_lanzaValidacion() {
        assertThatThrownBy(() ->
                service.resolver(denunciaId, req(ResolucionDenuncia.OCULTAR_RESENIA), UUID.randomUUID()))
                .isInstanceOf(ValidacionException.class);
    }

    // --- denuncias sobre una reseña (E2I-HU11) ---------------------------------------

    private Denuncia denunciaDeResenia(Resenia resenia, Usuario denunciante) {
        Denuncia d = new Denuncia();
        d.setResenia(resenia);
        d.setDenunciante(denunciante);
        d.setMotivo("Comentario agresivo");
        ReflectionTestUtils.setField(d, "id", denunciaId);
        when(denunciaRepository.findById(denunciaId)).thenReturn(Optional.of(d));
        return d;
    }

    private Resenia reseniaDelAlumno(UUID autorId) {
        Usuario autor = new Usuario();
        ReflectionTestUtils.setField(autor, "id", autorId);

        Actividad actividad = new Actividad();
        actividad.setNombre("Running");
        actividad.setInstructor(instructor);

        Clase clase = new Clase();
        clase.setActividad(actividad);
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());

        Resenia r = new Resenia();
        r.setClase(clase);
        r.setAlumno(autor);
        ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
        return r;
    }

    @Test
    void resolver_ocultarResenia_laOcultaYNotificaALasDosPartes() {
        UUID autorId = UUID.randomUUID();
        Resenia resenia = reseniaDelAlumno(autorId);
        denunciaDeResenia(resenia, instructor);

        ResolverDenunciaResponse response =
                service.resolver(denunciaId, req(ResolucionDenuncia.OCULTAR_RESENIA), UUID.randomUUID());

        assertThat(response.resolucion()).isEqualTo("OCULTAR_RESENIA");
        assertThat(resenia.isOculta()).isTrue();
        verify(reseniaRepository).save(resenia);
        verify(notificacionService).notificar(eq(instructorId), eq(TipoNotificacion.DENUNCIA_RESUELTA), any(), eq(denunciaId));
        verify(notificacionService).notificar(eq(autorId), eq(TipoNotificacion.DENUNCIA_RESUELTA), any(), eq(denunciaId));
    }

    @Test
    void resolver_desestimarDenunciaDeResenia_noLaOculta() {
        UUID autorId = UUID.randomUUID();
        Resenia resenia = reseniaDelAlumno(autorId);
        denunciaDeResenia(resenia, instructor);

        service.resolver(denunciaId, req(ResolucionDenuncia.DESESTIMAR), UUID.randomUUID());

        assertThat(resenia.isOculta()).isFalse();
        // Al autor de la reseña no se le avisa nada: su reseña sigue publicada.
        verify(notificacionService, never()).notificar(eq(autorId), any(), any(), any());
    }

    @Test
    void resolver_reintegrarSobreDenunciaDeResenia_lanzaValidacion() {
        // Sin esta guarda el switch entraba a reintegrar() y explotaba con NPE: la denuncia
        // de reseña no tiene clase ni alumno.
        denunciaDeResenia(reseniaDelAlumno(UUID.randomUUID()), instructor);

        assertThatThrownBy(() -> service.resolver(denunciaId, req(ResolucionDenuncia.REINTEGRAR), UUID.randomUUID()))
                .isInstanceOf(ValidacionException.class);

        verify(reseniaRepository, never()).save(any());
    }

    @Test
    void resolver_suspender_creaLaSuspensionConSuVigencia() {
        penalizacionConId();

        service.resolver(denunciaId, new ResolverDenunciaRequest("SUSPENDER", null, null, 30), UUID.randomUUID());

        ArgumentCaptor<com.activehub.domain.penalizacion.Penalizacion> captor =
                ArgumentCaptor.forClass(com.activehub.domain.penalizacion.Penalizacion.class);
        verify(penalizacionRepository).saveAndFlush(captor.capture());

        var suspension = captor.getValue();
        assertThat(suspension.getTipo()).isEqualTo(TipoPenalizacion.SUSPENSION_TEMPORAL);
        assertThat(suspension.getFechaInicio()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(suspension.getFechaFin()).isEqualTo(LocalDate.of(2026, 7, 1));
        // Queda atada a la denuncia que la originó: habilita el "Ver denuncia" del listado.
        assertThat(suspension.getDenuncia()).isEqualTo(denuncia);
        assertThat(instructor.getCantidadPenalizaciones()).isEqualTo(1);
    }

    @Test
    void resolver_suspenderConMulta_creaDosPenalizaciones() {
        penalizacionConId();

        service.resolver(
                denunciaId, new ResolverDenunciaRequest("SUSPENDER", null, new BigDecimal("12000"), 20),
                UUID.randomUUID());

        ArgumentCaptor<com.activehub.domain.penalizacion.Penalizacion> captor =
                ArgumentCaptor.forClass(com.activehub.domain.penalizacion.Penalizacion.class);
        verify(penalizacionRepository, org.mockito.Mockito.times(2)).saveAndFlush(captor.capture());

        assertThat(captor.getAllValues()).extracting(p -> p.getTipo())
                .containsExactly(TipoPenalizacion.SUSPENSION_TEMPORAL, TipoPenalizacion.ECONOMICA);
        assertThat(captor.getAllValues().get(1).getMonto()).isEqualByComparingTo("12000");
        assertThat(instructor.getCantidadPenalizaciones()).isEqualTo(2);
    }

    @Test
    void resolver_suspenderConMontoCero_noCreaLaEconomica() {
        // "Puede ser cero" = suspensión sin multa, no una multa de $0.
        penalizacionConId();

        service.resolver(
                denunciaId, new ResolverDenunciaRequest("SUSPENDER", null, BigDecimal.ZERO, 15), UUID.randomUUID());

        verify(penalizacionRepository, org.mockito.Mockito.times(1)).saveAndFlush(any());
        assertThat(instructor.getCantidadPenalizaciones()).isEqualTo(1);
    }

    @Test
    void resolver_suspenderSinDias_lanzaValidacion() {
        assertThatThrownBy(() -> service.resolver(
                denunciaId, new ResolverDenunciaRequest("SUSPENDER", null, null, null), UUID.randomUUID()))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("días");

        assertThat(instructor.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        verify(penalizacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void resolver_suspenderPorMenosDelMinimo_lanzaValidacion() {
        assertThatThrownBy(() -> service.resolver(
                denunciaId, new ResolverDenunciaRequest("SUSPENDER", null, null, 7), UUID.randomUUID()))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("15 días");

        assertThat(instructor.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        verify(denunciaRepository, never()).save(any());
    }
}
