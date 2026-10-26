package com.activehub.usecases.listardocumentosinstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.DocumentoInstructor;
import com.activehub.domain.usuario.DocumentoInstructorRepository;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.error.NoEncontradoException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarDocumentosInstructorServiceTest {

    @Mock
    private PerfilInstructorRepository perfilInstructorRepository;
    @Mock
    private DocumentoInstructorRepository documentoInstructorRepository;

    private ListarDocumentosInstructorService service;
    private UUID instructorId;
    private PerfilInstructor perfil;

    @BeforeEach
    void setUp() {
        service = new ListarDocumentosInstructorService(perfilInstructorRepository, documentoInstructorRepository);

        instructorId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", instructorId);
        perfil = new PerfilInstructor(usuario, "Yoga", 3, "d");
        ReflectionTestUtils.setField(perfil, "id", UUID.randomUUID());
    }

    @Test
    void listar_devuelveDocumentosDelInstructor() {
        DocumentoInstructor doc = new DocumentoInstructor();
        doc.setPerfilInstructor(perfil);
        doc.setTipoDocumento("application/pdf");
        doc.setNombreArchivo("cert.pdf");
        doc.setTamanioBytes(1234);
        ReflectionTestUtils.setField(doc, "id", UUID.randomUUID());

        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfil));
        when(documentoInstructorRepository.findByPerfilInstructorIdOrderByCreatedAtDesc(perfil.getId()))
                .thenReturn(List.of(doc));

        List<ListarDocumentosInstructorResponse> resultado = service.listar(instructorId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombreArchivo()).isEqualTo("cert.pdf");
        assertThat(resultado.get(0).tamanioBytes()).isEqualTo(1234);
    }

    /**
     * 2026-10-26: el admin ve también lo que el instructor retiró (historial) y qué documentos
     * llegaron después del último rechazo.
     */
    @Test
    void listar_marcaLosNuevosDesdeElRechazoYLosRetirados() {
        Instant rechazo = Instant.parse("2026-10-20T12:00:00Z");
        ReflectionTestUtils.setField(perfil, "rechazadoAt", rechazo);
        DocumentoInstructor viejo = documento("viejo.pdf", rechazo.minusSeconds(3600));
        viejo.setEstado(DocumentoInstructor.RETIRADO);
        DocumentoInstructor nuevo = documento("nuevo.pdf", rechazo.plusSeconds(3600));
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfil));
        when(documentoInstructorRepository.findByPerfilInstructorIdOrderByCreatedAtDesc(perfil.getId()))
                .thenReturn(List.of(nuevo, viejo));

        List<ListarDocumentosInstructorResponse> resultado = service.listar(instructorId);

        assertThat(resultado).extracting(ListarDocumentosInstructorResponse::nombreArchivo).containsExactly("nuevo.pdf", "viejo.pdf");
        assertThat(resultado.get(0).nuevo()).isTrue();
        assertThat(resultado.get(0).retirado()).isFalse();
        assertThat(resultado.get(1).nuevo()).isFalse();
        assertThat(resultado.get(1).retirado()).isTrue();
    }

    private DocumentoInstructor documento(String nombre, Instant creado) {
        DocumentoInstructor d = new DocumentoInstructor();
        d.setPerfilInstructor(perfil);
        d.setTipoDocumento("application/pdf");
        d.setNombreArchivo(nombre);
        ReflectionTestUtils.setField(d, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(d, "createdAt", creado);
        return d;
    }

    @Test
    void listar_instructorInexistente_lanzaNoEncontrado() {
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(instructorId)).isInstanceOf(NoEncontradoException.class);
    }
}
