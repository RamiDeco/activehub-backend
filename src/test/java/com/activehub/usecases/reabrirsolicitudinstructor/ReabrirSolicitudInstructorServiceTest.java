package com.activehub.usecases.reabrirsolicitudinstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.DocumentoInstructor;
import com.activehub.domain.usuario.DocumentoInstructorRepository;
import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.time.Instant;
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
class ReabrirSolicitudInstructorServiceTest {

    @Mock private PerfilInstructorRepository perfilInstructorRepository;
    @Mock private DocumentoInstructorRepository documentoInstructorRepository;
    @Mock private AuditService auditService;

    @InjectMocks private ReabrirSolicitudInstructorService service;

    private UUID instructorId;

    @BeforeEach
    void setUp() {
        instructorId = UUID.randomUUID();
    }

    private PerfilInstructor perfilCon(EstadoVerificacion estado, String motivo) {
        PerfilInstructor perfil = new PerfilInstructor();
        ReflectionTestUtils.setField(perfil, "estadoVerificacion", estado);
        ReflectionTestUtils.setField(perfil, "motivoRechazo", motivo);
        return perfil;
    }

    private static final Instant RECHAZO = Instant.parse("2026-10-20T12:00:00Z");

    /**
     * Con documentación subida después del rechazo vuelve a En revisión. El motivo y la fecha
     * del rechazo se conservan (2026-10-26): el administrador los ve al revisar otra vez.
     */
    @Test
    void reabrir_conDocumentacionNueva_vuelveAPendienteYConservaElMotivo() {
        PerfilInstructor perfil = perfilCon(EstadoVerificacion.RECHAZADO, "Documentación ilegible");
        ReflectionTestUtils.setField(perfil, "rechazadoAt", RECHAZO);
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfil));
        when(documentoInstructorRepository.existsByPerfilInstructorIdAndEstadoNotAndCreatedAtAfter(
                any(), eq(DocumentoInstructor.RETIRADO), eq(RECHAZO))).thenReturn(true);

        ReabrirSolicitudInstructorResponse response = service.reabrir(instructorId);

        assertThat(perfil.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE);
        assertThat(perfil.getMotivoRechazo()).isEqualTo("Documentación ilegible");
        assertThat(perfil.getRechazadoAt()).isEqualTo(RECHAZO);
        assertThat(response.estadoVerificacion()).isEqualTo("PENDIENTE");
        verify(perfilInstructorRepository).save(perfil);
        verify(auditService).registrar(any(), any(), any(), any(), any());
    }

    /**
     * Regla nueva (2026-10-26): sin documentación posterior al rechazo no se puede volver a
     * postular. Antes se reenviaba exactamente lo que el administrador había rechazado.
     */
    @Test
    void reabrir_sinDocumentacionNueva_lanzaValidacionYSigueRechazado() {
        PerfilInstructor perfil = perfilCon(EstadoVerificacion.RECHAZADO, "Falta el certificado");
        ReflectionTestUtils.setField(perfil, "rechazadoAt", RECHAZO);
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfil));
        when(documentoInstructorRepository.existsByPerfilInstructorIdAndEstadoNotAndCreatedAtAfter(
                any(), eq(DocumentoInstructor.RETIRADO), eq(RECHAZO))).thenReturn(false);

        assertThatThrownBy(() -> service.reabrir(instructorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("documentación nueva");
        assertThat(perfil.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.RECHAZADO);
        verify(perfilInstructorRepository, never()).save(any());
    }

    @Test
    void reabrir_desdePendiente_lanzaValidacion() {
        when(perfilInstructorRepository.findByUsuarioId(instructorId))
                .thenReturn(Optional.of(perfilCon(EstadoVerificacion.PENDIENTE, null)));

        assertThatThrownBy(() -> service.reabrir(instructorId)).isInstanceOf(ValidacionException.class);
        verify(perfilInstructorRepository, never()).save(any());
    }

    @Test
    void reabrir_desdeAprobado_lanzaValidacion() {
        // Un instructor aprobado no tiene nada que volver a postular.
        when(perfilInstructorRepository.findByUsuarioId(instructorId))
                .thenReturn(Optional.of(perfilCon(EstadoVerificacion.APROBADO, null)));

        assertThatThrownBy(() -> service.reabrir(instructorId)).isInstanceOf(ValidacionException.class);
    }

    @Test
    void reabrir_sinPerfil_lanzaNoEncontrado() {
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reabrir(instructorId)).isInstanceOf(NoEncontradoException.class);
    }
}
