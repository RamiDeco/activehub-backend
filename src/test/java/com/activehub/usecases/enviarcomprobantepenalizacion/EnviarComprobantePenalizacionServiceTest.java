package com.activehub.usecases.enviarcomprobantepenalizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.activehub.domain.permiso.ConfiguracionRolRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.storage.AlmacenamientoDisco;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * V36: el instructor manda el comprobante de la transferencia con la que pagó su multa. La
 * multa pasa a "En revisión" y se avisa a quienes revisan penalizaciones.
 */
@ExtendWith(MockitoExtension.class)
class EnviarComprobantePenalizacionServiceTest {

    @Mock private PenalizacionRepository penalizacionRepository;
    @Mock private ComprobantePenalizacionRepository comprobantePenalizacionRepository;
    @Mock private ConfiguracionRolRepository configuracionRolRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private AuditService auditService;

    @TempDir Path tempDir;

    private EnviarComprobantePenalizacionService service;
    private UUID instructorId;
    private Penalizacion multa;

    private final MockMultipartFile pdf =
            new MockMultipartFile("archivo", "transferencia.pdf", "application/pdf", "contenido".getBytes());

    @BeforeEach
    void setUp() {
        service = new EnviarComprobantePenalizacionService(
                penalizacionRepository, comprobantePenalizacionRepository, configuracionRolRepository,
                usuarioRepository, notificacionService, auditService,
                new AlmacenamientoDisco(tempDir.toString(), tempDir.toString(), tempDir.toString(), tempDir.toString()));

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        instructor.setNombre("Lucía");
        instructor.setApellido("Fernández");
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        multa = new Penalizacion();
        multa.setUsuario(instructor);
        multa.setTipo(TipoPenalizacion.ECONOMICA);
        multa.setMonto(new BigDecimal("5000"));
        multa.setEstadoPago(EstadoPagoPenalizacion.PENDIENTE_PAGO);
        ReflectionTestUtils.setField(multa, "id", UUID.randomUUID());
    }

    private void comprobanteConId() {
        when(comprobantePenalizacionRepository.save(any())).thenAnswer(inv -> {
            ComprobantePenalizacion c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });
    }

    @Test
    void enviar_multaPendiente_guardaElArchivoPasaAEnRevisionYAvisaALaAdministracion() throws Exception {
        UUID rolAdmin = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        when(penalizacionRepository.findById(multa.getId())).thenReturn(Optional.of(multa));
        comprobanteConId();
        when(configuracionRolRepository.rolesConPermiso("penalizaciones.gestionar")).thenReturn(List.of(rolAdmin));
        when(usuarioRepository.findIdsActivosPorRol(List.of(rolAdmin))).thenReturn(List.of(adminId));

        EnviarComprobantePenalizacionResponse response = service.enviar(multa.getId(), pdf, instructorId);

        assertThat(response.estadoPago()).isEqualTo("EN_REVISION");
        assertThat(multa.getEstadoPago()).isEqualTo(EstadoPagoPenalizacion.EN_REVISION);

        ArgumentCaptor<ComprobantePenalizacion> captor = ArgumentCaptor.forClass(ComprobantePenalizacion.class);
        verify(comprobantePenalizacionRepository).save(captor.capture());
        ComprobantePenalizacion comprobante = captor.getValue();
        assertThat(comprobante.getEstado()).isEqualTo(EstadoComprobantePenalizacion.EN_REVISION);
        assertThat(comprobante.getNombreArchivo()).isEqualTo("transferencia.pdf");
        try (var archivos = Files.list(tempDir)) {
            assertThat(archivos).hasSize(1);
        }

        verify(auditService).registrar(
                eq(instructorId), eq(AuditAccion.COMPROBANTE_PENALIZACION_ENVIADO), eq("Penalizacion"), eq(multa.getId()), any());
        verify(notificacionService).notificar(eq(adminId), eq(TipoNotificacion.COMPROBANTE_PENALIZACION_ENVIADO),
                any(), eq(multa.getId()), eq(Destino.penalizacion(multa.getId())));
    }

    @Test
    void enviar_multaAjena_lanzaNoEncontrado() {
        when(penalizacionRepository.findById(multa.getId())).thenReturn(Optional.of(multa));

        assertThatThrownBy(() -> service.enviar(multa.getId(), pdf, UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
        verify(comprobantePenalizacionRepository, never()).save(any());
    }

    @Test
    void enviar_conUnComprobanteYaEnRevision_lanzaValidacion() {
        multa.setEstadoPago(EstadoPagoPenalizacion.EN_REVISION);
        when(penalizacionRepository.findById(multa.getId())).thenReturn(Optional.of(multa));

        assertThatThrownBy(() -> service.enviar(multa.getId(), pdf, instructorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("esperá");
    }

    @Test
    void enviar_multaYaPagada_lanzaValidacion() {
        multa.setEstadoPago(EstadoPagoPenalizacion.PAGADA);
        when(penalizacionRepository.findById(multa.getId())).thenReturn(Optional.of(multa));

        assertThatThrownBy(() -> service.enviar(multa.getId(), pdf, instructorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("pagada");
    }

    /** Una multa anterior a V36 (o una suspensión) no tiene estado de pago: no se paga por acá. */
    @Test
    void enviar_penalizacionSinEstadoDePago_lanzaValidacion() {
        multa.setEstadoPago(null);
        when(penalizacionRepository.findById(multa.getId())).thenReturn(Optional.of(multa));

        assertThatThrownBy(() -> service.enviar(multa.getId(), pdf, instructorId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void enviar_formatoNoPermitido_lanzaValidacionYNoCambiaElEstado() {
        when(penalizacionRepository.findById(multa.getId())).thenReturn(Optional.of(multa));
        var exe = new MockMultipartFile("archivo", "virus.exe", "application/octet-stream", "x".getBytes());

        assertThatThrownBy(() -> service.enviar(multa.getId(), exe, instructorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("PDF, JPG o PNG");
        assertThat(multa.getEstadoPago()).isEqualTo(EstadoPagoPenalizacion.PENDIENTE_PAGO);
    }
}
