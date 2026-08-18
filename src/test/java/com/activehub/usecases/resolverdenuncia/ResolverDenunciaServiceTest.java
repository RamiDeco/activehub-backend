package com.activehub.usecases.resolverdenuncia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.payments.PaymentGateway;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ResolverDenunciaServiceTest {

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
                paymentGateway, auditService);

        instructorId = UUID.randomUUID();
        instructor = new Usuario();
        instructor.setCantidadPenalizaciones(0);
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setInstructor(instructor);

        claseId = UUID.randomUUID();
        Clase clase = new Clase();
        clase.setActividad(actividad);
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

    private ResolverDenunciaRequest req(AccionResolucion accion) {
        return new ResolverDenunciaRequest(accion.name());
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

        ResolverDenunciaResponse response = service.resolver(denunciaId, req(AccionResolucion.REINTEGRAR), UUID.randomUUID());

        assertThat(response.estado()).isEqualTo("Resuelta");
        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Cancelado);
        verify(paymentGateway).cancelarPago("ref-1");
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

        service.resolver(denunciaId, req(AccionResolucion.REINTEGRAR), UUID.randomUUID());

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Efectivo);
        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
        verify(paymentGateway, never()).cancelarPago(any());
    }

    @Test
    void resolver_suspender_afectaAlInstructorNoAlAlumno() {
        ResolverDenunciaResponse response = service.resolver(denunciaId, req(AccionResolucion.SUSPENDER), UUID.randomUUID());

        assertThat(response.estado()).isEqualTo("Resuelta");
        assertThat(instructor.getEstado()).isEqualTo(EstadoUsuario.SUSPENDIDO);
        verify(usuarioRepository).save(instructor);
    }

    @Test
    void resolver_penalizar_creaPenalizacionYSumaContadorDelInstructor() {
        service.resolver(denunciaId, req(AccionResolucion.PENALIZAR), UUID.randomUUID());

        assertThat(instructor.getCantidadPenalizaciones()).isEqualTo(1);
        verify(penalizacionRepository).save(any());
        verify(usuarioRepository).save(instructor);
    }

    @Test
    void resolver_desestimar_soloCierraElCaso() {
        ResolverDenunciaResponse response = service.resolver(denunciaId, req(AccionResolucion.DESESTIMAR), UUID.randomUUID());

        assertThat(response.estado()).isEqualTo("Resuelta");
        assertThat(instructor.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(instructor.getCantidadPenalizaciones()).isEqualTo(0);
    }

    @Test
    void resolver_yaResuelta_lanzaValidacion() {
        denuncia.setEstado(EstadoDenuncia.RESUELTA);

        assertThatThrownBy(() -> service.resolver(denunciaId, req(AccionResolucion.DESESTIMAR), UUID.randomUUID()))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void resolver_accionInvalida_lanzaValidacion() {
        assertThatThrownBy(() -> service.resolver(denunciaId, new ResolverDenunciaRequest("NO_EXISTE"), UUID.randomUUID()))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void resolver_inexistente_lanzaNoEncontrado() {
        when(denunciaRepository.findById(denunciaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolver(denunciaId, req(AccionResolucion.DESESTIMAR), UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
