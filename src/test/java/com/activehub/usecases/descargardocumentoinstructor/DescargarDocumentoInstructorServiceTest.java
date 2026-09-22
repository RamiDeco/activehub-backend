package com.activehub.usecases.descargardocumentoinstructor;

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

@ExtendWith(MockitoExtension.class)
class DescargarDocumentoInstructorServiceTest {

    @Mock
    private PerfilInstructorRepository perfilInstructorRepository;
    @Mock
    private DocumentoInstructorRepository documentoInstructorRepository;

    @TempDir
    Path tempDir;

    private DescargarDocumentoInstructorService service;
    private UUID instructorId;
    private UUID documentoId;
    private PerfilInstructor perfil;

    @BeforeEach
    void setUp() {
        AlmacenamientoDisco almacenamiento =
                new AlmacenamientoDisco(tempDir.toString(), tempDir.toString(), tempDir.toString());
        service = new DescargarDocumentoInstructorService(
                perfilInstructorRepository, documentoInstructorRepository, almacenamiento);

        instructorId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", instructorId);
        perfil = new PerfilInstructor(usuario, "Yoga", 3, "d");
        ReflectionTestUtils.setField(perfil, "id", UUID.randomUUID());
        documentoId = UUID.randomUUID();
    }

    @Test
    void descargar_documentoExistente_devuelveContenido() throws Exception {
        Files.writeString(tempDir.resolve("archivo-en-disco.pdf"), "contenido real del pdf");

        DocumentoInstructor doc = new DocumentoInstructor();
        doc.setPerfilInstructor(perfil);
        doc.setTipoDocumento("application/pdf");
        doc.setNombreArchivo("cert.pdf");
        doc.setRutaArchivo("archivo-en-disco.pdf");
        ReflectionTestUtils.setField(doc, "id", documentoId);

        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfil));
        when(documentoInstructorRepository.findByIdAndPerfilInstructorId(documentoId, perfil.getId()))
                .thenReturn(Optional.of(doc));

        DocumentoDescarga descarga = service.descargar(instructorId, documentoId);

        assertThat(descarga.nombreArchivo()).isEqualTo("cert.pdf");
        assertThat(descarga.tipoContenido()).isEqualTo("application/pdf");
        assertThat(new String(descarga.contenido())).isEqualTo("contenido real del pdf");
    }

    @Test
    void descargar_documentoDeOtroInstructor_lanzaNoEncontrado() {
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfil));
        when(documentoInstructorRepository.findByIdAndPerfilInstructorId(documentoId, perfil.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.descargar(instructorId, documentoId))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void descargar_instructorInexistente_lanzaNoEncontrado() {
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.descargar(instructorId, documentoId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
