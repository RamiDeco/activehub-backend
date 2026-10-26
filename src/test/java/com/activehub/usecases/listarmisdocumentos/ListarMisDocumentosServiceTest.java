package com.activehub.usecases.listarmisdocumentos;

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

/** El instructor ve su propia documentación (2026-10-26). */
@ExtendWith(MockitoExtension.class)
class ListarMisDocumentosServiceTest {

    @Mock
    private PerfilInstructorRepository perfilInstructorRepository;
    @Mock
    private DocumentoInstructorRepository documentoInstructorRepository;

    private ListarMisDocumentosService service;
    private UUID instructorId;
    private PerfilInstructor perfil;

    @BeforeEach
    void setUp() {
        service = new ListarMisDocumentosService(perfilInstructorRepository, documentoInstructorRepository);
        instructorId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", instructorId);
        perfil = new PerfilInstructor(usuario, "Yoga", 3, "d");
        ReflectionTestUtils.setField(perfil, "id", UUID.randomUUID());
    }

    private DocumentoInstructor documento(String nombre, Instant creado) {
        DocumentoInstructor d = new DocumentoInstructor();
        d.setPerfilInstructor(perfil);
        d.setTipoDocumento("application/pdf");
        d.setNombreArchivo(nombre);
        d.setTamanioBytes(100);
        ReflectionTestUtils.setField(d, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(d, "createdAt", creado);
        return d;
    }

    @Test
    void listar_devuelveLosVigentesSinLosRetiradosYMarcaLosPosterioresAlRechazo() {
        Instant rechazo = Instant.parse("2026-10-20T12:00:00Z");
        ReflectionTestUtils.setField(perfil, "rechazadoAt", rechazo);
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfil));
        // El repositorio ya excluye los RETIRADO: el test fija que se pida así.
        when(documentoInstructorRepository.findByPerfilInstructorIdAndEstadoNotOrderByCreatedAtDesc(
                perfil.getId(), DocumentoInstructor.RETIRADO))
                .thenReturn(List.of(documento("nuevo.pdf", rechazo.plusSeconds(60)), documento("viejo.pdf", rechazo.minusSeconds(60))));

        List<ListarMisDocumentosResponse> resultado = service.listar(instructorId);

        assertThat(resultado).extracting(ListarMisDocumentosResponse::nombreArchivo).containsExactly("nuevo.pdf", "viejo.pdf");
        assertThat(resultado).extracting(ListarMisDocumentosResponse::nuevo).containsExactly(true, false);
    }

    @Test
    void listar_sinRechazoNingunoEsNuevo() {
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfil));
        when(documentoInstructorRepository.findByPerfilInstructorIdAndEstadoNotOrderByCreatedAtDesc(
                perfil.getId(), DocumentoInstructor.RETIRADO))
                .thenReturn(List.of(documento("cert.pdf", Instant.now())));

        assertThat(service.listar(instructorId)).extracting(ListarMisDocumentosResponse::nuevo).containsExactly(false);
    }

    @Test
    void listar_sinPerfilDeInstructor_lanzaNoEncontrado() {
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(instructorId)).isInstanceOf(NoEncontradoException.class);
    }
}
