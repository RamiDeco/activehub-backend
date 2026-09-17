package com.activehub.usecases.crearreportesoporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.activehub.domain.soporte.EstadoReporteSoporte;
import com.activehub.domain.soporte.ReporteSoporte;
import com.activehub.domain.soporte.ReporteSoporteRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
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
class CrearReporteSoporteServiceTest {

    @Mock
    private ReporteSoporteRepository reporteSoporteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditService auditService;

    private CrearReporteSoporteService service;

    @BeforeEach
    void setUp() {
        service = new CrearReporteSoporteService(reporteSoporteRepository, usuarioRepository, auditService);
        when(reporteSoporteRepository.saveAndFlush(any(ReporteSoporte.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private CrearReporteSoporteRequest request() {
        return new CrearReporteSoporteRequest("  Ana@Mail.COM ", "  No puedo pagar  ", "  Me tira error 500.  ");
    }

    private ReporteSoporte guardado() {
        ArgumentCaptor<ReporteSoporte> captor = ArgumentCaptor.forClass(ReporteSoporte.class);
        verify(reporteSoporteRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    void crear_normalizaLosCamposYNaceAbierto() {
        service.crear(request(), null);

        ReporteSoporte r = guardado();
        assertThat(r.getEmail()).isEqualTo("ana@mail.com");
        assertThat(r.getAsunto()).isEqualTo("No puedo pagar");
        assertThat(r.getDetalle()).isEqualTo("Me tira error 500.");
        assertThat(r.getEstado()).isEqualTo(EstadoReporteSoporte.ABIERTO);
    }

    @Test
    void crear_conUsuarioLogueado_guardaElAutor() {
        UUID autorId = UUID.randomUUID();
        Usuario autor = new Usuario();
        ReflectionTestUtils.setField(autor, "id", autorId);
        when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(autor));

        service.crear(request(), autorId);

        assertThat(guardado().getUsuario()).isSameAs(autor);
        verify(auditService).registrar(
                eq(autorId), eq(AuditAccion.REPORTE_SOPORTE_CREADO), eq("ReporteSoporte"), any(), isNull());
    }

    /**
     * El endpoint es publico: que no haya sesion es el caso normal, no un error. Es justamente
     * quien no pudo registrarse el que mas necesita el formulario.
     */
    @Test
    void crear_sinSesion_loGuardaComoAnonimo() {
        service.crear(request(), null);

        assertThat(guardado().getUsuario()).isNull();
        verifyNoInteractions(usuarioRepository);
    }

    /** Token vivo de una cuenta ya dada de baja: el reporte se guarda igual, sin autor. */
    @Test
    void crear_conAutorInexistente_loGuardaComoAnonimo() {
        UUID autorId = UUID.randomUUID();
        when(usuarioRepository.findById(autorId)).thenReturn(Optional.empty());

        service.crear(request(), autorId);

        assertThat(guardado().getUsuario()).isNull();
        verify(reporteSoporteRepository, never()).delete(any());
    }
}
