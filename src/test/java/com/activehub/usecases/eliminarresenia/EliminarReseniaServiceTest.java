package com.activehub.usecases.eliminarresenia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EliminarReseniaServiceTest {

    @Mock
    private ReseniaRepository reseniaRepository;
    @Mock
    private AuditService auditService;

    private EliminarReseniaService service;
    private UUID alumnoId;
    private UUID reseniaId;
    private Resenia resenia;

    @BeforeEach
    void setUp() {
        service = new EliminarReseniaService(reseniaRepository, auditService);

        alumnoId = UUID.randomUUID();
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        reseniaId = UUID.randomUUID();
        resenia = new Resenia();
        resenia.setAlumno(alumno);
        ReflectionTestUtils.setField(resenia, "id", reseniaId);
    }

    @Test
    void eliminar_propia_marcaBorrada() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        service.eliminar(reseniaId, alumnoId);

        assertThat(resenia.isDeleted()).isTrue();
    }

    @Test
    void eliminar_ajena_lanzaSinPermiso() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        assertThatThrownBy(() -> service.eliminar(reseniaId, UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void eliminar_inexistente_lanzaNoEncontrado() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(reseniaId, alumnoId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
