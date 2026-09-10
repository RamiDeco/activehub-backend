package com.activehub.usecases.reabrirsolicitudinstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
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

    @Test
    void reabrir_desdeRechazado_vuelveAPendienteYLimpiaElMotivo() {
        PerfilInstructor perfil = perfilCon(EstadoVerificacion.RECHAZADO, "Documentación ilegible");
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfil));

        ReabrirSolicitudInstructorResponse response = service.reabrir(instructorId);

        assertThat(perfil.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE);
        assertThat(perfil.getMotivoRechazo()).isNull();
        assertThat(response.estadoVerificacion()).isEqualTo("PENDIENTE");
        verify(perfilInstructorRepository).save(perfil);
        verify(auditService).registrar(any(), any(), any(), any(), any());
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
