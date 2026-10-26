package com.activehub.usecases.retirarmidocumento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.DocumentoInstructor;
import com.activehub.domain.usuario.DocumentoInstructorRepository;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** El instructor quita un documento propio: queda RETIRADO, no se borra (2026-10-26). */
@ExtendWith(MockitoExtension.class)
class RetirarMiDocumentoServiceTest {

    @Mock
    private PerfilInstructorRepository perfilInstructorRepository;
    @Mock
    private DocumentoInstructorRepository documentoInstructorRepository;
    @Mock
    private AuditService auditService;

    private RetirarMiDocumentoService service;
    private UUID instructorId;
    private PerfilInstructor perfil;
    private DocumentoInstructor documento;

    @BeforeEach
    void setUp() {
        service = new RetirarMiDocumentoService(perfilInstructorRepository, documentoInstructorRepository, auditService);
        instructorId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", instructorId);
        perfil = new PerfilInstructor(usuario, "Yoga", 3, "d");
        ReflectionTestUtils.setField(perfil, "id", UUID.randomUUID());
        documento = new DocumentoInstructor();
        documento.setPerfilInstructor(perfil);
        ReflectionTestUtils.setField(documento, "id", UUID.randomUUID());
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfil));
    }

    @Test
    void retirar_conOtrosDocumentos_loMarcaRetiradoSinBorrarloYAudita() {
        when(documentoInstructorRepository.findByIdAndPerfilInstructorId(documento.getId(), perfil.getId()))
                .thenReturn(Optional.of(documento));
        when(documentoInstructorRepository.countByPerfilInstructorIdAndEstadoNot(perfil.getId(), DocumentoInstructor.RETIRADO))
                .thenReturn(2L);

        service.retirar(instructorId, documento.getId());

        assertThat(documento.isRetirado()).isTrue();
        assertThat(documento.isDeleted()).isFalse();
        verify(documentoInstructorRepository).save(documento);
        verify(auditService).registrar(
                eq(instructorId), eq(AuditAccion.DOCUMENTO_INSTRUCTOR_RETIRADO), eq("DocumentoInstructor"), eq(documento.getId()), any());
    }

    @Test
    void retirar_elUltimoDocumento_lanzaValidacion() {
        when(documentoInstructorRepository.findByIdAndPerfilInstructorId(documento.getId(), perfil.getId()))
                .thenReturn(Optional.of(documento));
        when(documentoInstructorRepository.countByPerfilInstructorIdAndEstadoNot(perfil.getId(), DocumentoInstructor.RETIRADO))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.retirar(instructorId, documento.getId()))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("al menos un documento");
        assertThat(documento.isRetirado()).isFalse();
        verify(documentoInstructorRepository, never()).save(any());
    }

    @Test
    void retirar_ajenoOYaRetirado_lanzaNoEncontrado() {
        UUID ajeno = UUID.randomUUID();
        when(documentoInstructorRepository.findByIdAndPerfilInstructorId(ajeno, perfil.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.retirar(instructorId, ajeno)).isInstanceOf(NoEncontradoException.class);

        documento.setEstado(DocumentoInstructor.RETIRADO);
        when(documentoInstructorRepository.findByIdAndPerfilInstructorId(documento.getId(), perfil.getId()))
                .thenReturn(Optional.of(documento));
        assertThatThrownBy(() -> service.retirar(instructorId, documento.getId())).isInstanceOf(NoEncontradoException.class);
    }
}
