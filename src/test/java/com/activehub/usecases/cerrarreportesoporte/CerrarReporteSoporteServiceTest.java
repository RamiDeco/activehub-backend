package com.activehub.usecases.cerrarreportesoporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.soporte.EstadoReporteSoporte;
import com.activehub.domain.soporte.ReporteSoporte;
import com.activehub.domain.soporte.ReporteSoporteRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.time.Clock;
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
class CerrarReporteSoporteServiceTest {

    private static final Instant AHORA = Instant.parse("2026-09-15T12:00:00Z");

    @Mock
    private ReporteSoporteRepository reporteSoporteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditService auditService;

    private CerrarReporteSoporteService service;
    private UUID reporteId;
    private UUID actorId;
    private ReporteSoporte reporte;

    @BeforeEach
    void setUp() {
        service = new CerrarReporteSoporteService(
                reporteSoporteRepository, usuarioRepository, auditService, Clock.fixed(AHORA, ZoneOffset.UTC));

        reporteId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        reporte = new ReporteSoporte();
        ReflectionTestUtils.setField(reporte, "id", reporteId);
        reporte.setEmail("ana@mail.com");
        reporte.setAsunto("No puedo pagar");
        reporte.setDetalle("Me tira error.");
    }

    @Test
    void cerrar_reporteAbierto_loCierraConFechaYAutor() {
        Usuario admin = new Usuario();
        ReflectionTestUtils.setField(admin, "id", actorId);
        when(reporteSoporteRepository.findById(reporteId)).thenReturn(Optional.of(reporte));
        when(usuarioRepository.findById(actorId)).thenReturn(Optional.of(admin));

        var response = service.cerrar(reporteId, new CerrarReporteSoporteRequest("  Ya está resuelto.  "), actorId);

        assertThat(reporte.getEstado()).isEqualTo(EstadoReporteSoporte.CERRADO);
        assertThat(reporte.getRespuesta()).isEqualTo("Ya está resuelto.");
        assertThat(reporte.getCerradoAt()).isEqualTo(AHORA);
        assertThat(reporte.getCerradoPor()).isSameAs(admin);
        assertThat(response.estado()).isEqualTo("Cerrado");
        verify(auditService).registrar(
                eq(actorId), eq(AuditAccion.REPORTE_SOPORTE_CERRADO), eq("ReporteSoporte"), eq(reporteId), isNull());
    }

    /** Se puede cerrar sin escribir nada; el blanco se normaliza a null, no a cadena vacia. */
    @Test
    void cerrar_sinRespuesta_dejaLaRespuestaEnNull() {
        when(reporteSoporteRepository.findById(reporteId)).thenReturn(Optional.of(reporte));
        when(usuarioRepository.findById(actorId)).thenReturn(Optional.empty());

        service.cerrar(reporteId, new CerrarReporteSoporteRequest("   "), actorId);

        assertThat(reporte.getEstado()).isEqualTo(EstadoReporteSoporte.CERRADO);
        assertThat(reporte.getRespuesta()).isNull();
    }

    /**
     * El estado solo avanza, igual que en las denuncias: volver a cerrar pisaria la respuesta
     * y la fecha del cierre original, que es justo lo que hay que conservar.
     */
    @Test
    void cerrar_reporteYaCerrado_lanzaValidacion() {
        reporte.setEstado(EstadoReporteSoporte.CERRADO);
        reporte.setRespuesta("La primera respuesta");
        when(reporteSoporteRepository.findById(reporteId)).thenReturn(Optional.of(reporte));

        assertThatThrownBy(() -> service.cerrar(reporteId, new CerrarReporteSoporteRequest("otra"), actorId))
                .isInstanceOf(ValidacionException.class);

        assertThat(reporte.getRespuesta()).isEqualTo("La primera respuesta");
        verify(reporteSoporteRepository, never()).save(any());
    }

    @Test
    void cerrar_inexistente_lanzaNoEncontrado() {
        when(reporteSoporteRepository.findById(reporteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cerrar(reporteId, new CerrarReporteSoporteRequest(null), actorId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
