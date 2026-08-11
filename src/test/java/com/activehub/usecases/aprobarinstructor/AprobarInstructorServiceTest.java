package com.activehub.usecases.aprobarinstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AprobarInstructorServiceTest {

    @Mock
    private PerfilInstructorRepository perfilInstructorRepository;
    @Mock
    private AuditService auditService;

    private AprobarInstructorService service;
    private UUID usuarioId;
    private PerfilInstructor perfil;

    @BeforeEach
    void setUp() {
        service = new AprobarInstructorService(perfilInstructorRepository, auditService);

        usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", usuarioId);

        perfil = new PerfilInstructor(usuario, "Yoga", 5, "desc");
        perfil.setMotivoRechazo("rechazado antes");
    }

    @Test
    void aprobar_instructorExistente_cambiaEstadoYLimpiaMotivo() {
        when(perfilInstructorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));

        AprobarInstructorResponse response = service.aprobar(usuarioId, UUID.randomUUID());

        assertThat(response.estadoVerificacion()).isEqualTo("APROBADO");
        assertThat(perfil.getMotivoRechazo()).isNull();
        verify(auditService).registrar(any(UUID.class), eq(AuditAccion.INSTRUCTOR_VALIDADO), eq("PerfilInstructor"), eq(usuarioId), isNull());
    }

    @Test
    void aprobar_instructorInexistente_lanzaNoEncontrado() {
        when(perfilInstructorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aprobar(usuarioId, UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
