package com.activehub.usecases.subirdocumento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import com.activehub.shared.storage.AlmacenamientoDisco;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubirDocumentoServiceTest {

    @Mock
    private PerfilInstructorRepository perfilInstructorRepository;
    @Mock
    private DocumentoInstructorRepository documentoInstructorRepository;
    @Mock
    private AuditService auditService;

    @TempDir
    Path tempDir;

    private SubirDocumentoService service;
    private UUID instructorId;
    private PerfilInstructor perfil;

    @BeforeEach
    void setUp() {
        AlmacenamientoDisco almacenamiento =
                new AlmacenamientoDisco(tempDir.toString(), tempDir.toString(), tempDir.toString());
        service = new SubirDocumentoService(
                perfilInstructorRepository, documentoInstructorRepository, auditService, almacenamiento);

        instructorId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", instructorId);
        perfil = new PerfilInstructor(usuario, "Yoga", 3, "d");
        ReflectionTestUtils.setField(perfil, "id", UUID.randomUUID());
    }

    @Test
    void subir_pdfValido_creaDocumentoYAudita() {
        MockMultipartFile archivo =
                new MockMultipartFile("archivo", "cert.pdf", "application/pdf", "contenido".getBytes());
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfil));
        when(documentoInstructorRepository.save(any(DocumentoInstructor.class))).thenAnswer(inv -> {
            DocumentoInstructor d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", UUID.randomUUID());
            return d;
        });

        SubirDocumentoResponse response = service.subir(archivo, instructorId);

        assertThat(response.nombreArchivo()).isEqualTo("cert.pdf");
        assertThat(response.tipoDocumento()).isEqualTo("application/pdf");
        assertThat(response.tamanioBytes()).isEqualTo(archivo.getSize());
        verify(auditService).registrar(
                eq(instructorId), eq(AuditAccion.DOCUMENTO_INSTRUCTOR_SUBIDO), eq("DocumentoInstructor"), any(), isNull());
    }

    @Test
    void subir_formatoNoPermitido_lanzaValidacion() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "video.mp4", "video/mp4", "x".getBytes());

        assertThatThrownBy(() -> service.subir(archivo, instructorId)).isInstanceOf(ValidacionException.class);
    }

    @Test
    void subir_archivoVacio_lanzaValidacion() {
        MockMultipartFile vacio = new MockMultipartFile("archivo", "cert.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.subir(vacio, instructorId)).isInstanceOf(ValidacionException.class);
    }

    @Test
    void subir_instructorInexistente_lanzaNoEncontrado() {
        MockMultipartFile archivo =
                new MockMultipartFile("archivo", "cert.pdf", "application/pdf", "contenido".getBytes());
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.subir(archivo, instructorId)).isInstanceOf(NoEncontradoException.class);
    }
}
