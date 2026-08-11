package com.activehub.usecases.rechazarinstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RechazarInstructorServiceTest {

    @Mock
    private PerfilInstructorRepository perfilInstructorRepository;
    @Mock
    private AuditService auditService;

    private RechazarInstructorService service;
    private UUID usuarioId;
    private PerfilInstructor perfil;

    @BeforeEach
    void setUp() {
        service = new RechazarInstructorService(perfilInstructorRepository, auditService);

        usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
        perfil = new PerfilInstructor(usuario, "Yoga", 5, "desc");
    }

    @Test
    void rechazar_conMotivo_guardaMotivoYCambiaEstado() {
        when(perfilInstructorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));

        RechazarInstructorResponse response = service.rechazar(
                usuarioId, new RechazarInstructorRequest("Documentación incompleta"), UUID.randomUUID());

        assertThat(response.estadoVerificacion()).isEqualTo("RECHAZADO");
        assertThat(response.motivoRechazo()).isEqualTo("Documentación incompleta");
        verify(auditService).registrar(any(UUID.class), eq(AuditAccion.INSTRUCTOR_RECHAZADO), eq("PerfilInstructor"), eq(usuarioId), eq("Documentación incompleta"));
    }

    @Test
    void rechazar_sinMotivo_quedaEnNull() {
        when(perfilInstructorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));

        RechazarInstructorResponse response = service.rechazar(usuarioId, new RechazarInstructorRequest(null), UUID.randomUUID());

        assertThat(response.motivoRechazo()).isNull();
    }
}
