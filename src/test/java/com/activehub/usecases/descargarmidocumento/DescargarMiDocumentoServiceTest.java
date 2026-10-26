package com.activehub.usecases.descargarmidocumento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.DocumentoInstructor;
import com.activehub.domain.usuario.DocumentoInstructorRepository;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.storage.AlmacenamientoDisco;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** El instructor abre un documento propio (2026-10-26). */
@ExtendWith(MockitoExtension.class)
class DescargarMiDocumentoServiceTest {

    @Mock
    private PerfilInstructorRepository perfilInstructorRepository;
    @Mock
    private DocumentoInstructorRepository documentoInstructorRepository;

    @TempDir
    Path tempDir;

    private DescargarMiDocumentoService service;
    private UUID instructorId;
    private PerfilInstructor perfil;
    private DocumentoInstructor documento;

    @BeforeEach
    void setUp() throws Exception {
        service = new DescargarMiDocumentoService(perfilInstructorRepository, documentoInstructorRepository,
                new AlmacenamientoDisco(tempDir.toString(), tempDir.toString(), tempDir.toString()));
        instructorId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", instructorId);
        perfil = new PerfilInstructor(usuario, "Yoga", 3, "d");
        ReflectionTestUtils.setField(perfil, "id", UUID.randomUUID());

        Files.writeString(tempDir.resolve("guardado.pdf"), "contenido");
        documento = new DocumentoInstructor();
        documento.setPerfilInstructor(perfil);
        documento.setTipoDocumento("application/pdf");
        documento.setNombreArchivo("certificado.pdf");
        documento.setRutaArchivo("guardado.pdf");
        ReflectionTestUtils.setField(documento, "id", UUID.randomUUID());
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfil));
    }

    @Test
    void descargar_documentoPropio_devuelveElContenido() {
        when(documentoInstructorRepository.findByIdAndPerfilInstructorId(documento.getId(), perfil.getId()))
                .thenReturn(Optional.of(documento));

        DescargarMiDocumentoResponse r = service.descargar(instructorId, documento.getId());

        assertThat(r.nombreArchivo()).isEqualTo("certificado.pdf");
        assertThat(new String(r.contenido())).isEqualTo("contenido");
    }

    @Test
    void descargar_documentoAjenoORetirado_lanzaNoEncontrado() {
        // Ajeno: el repositorio lo busca junto con el perfil del que llama y no lo encuentra.
        UUID ajeno = UUID.randomUUID();
        when(documentoInstructorRepository.findByIdAndPerfilInstructorId(ajeno, perfil.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.descargar(instructorId, ajeno)).isInstanceOf(NoEncontradoException.class);

        documento.setEstado(DocumentoInstructor.RETIRADO);
        when(documentoInstructorRepository.findByIdAndPerfilInstructorId(documento.getId(), perfil.getId()))
                .thenReturn(Optional.of(documento));
        assertThatThrownBy(() -> service.descargar(instructorId, documento.getId())).isInstanceOf(NoEncontradoException.class);
    }
}
