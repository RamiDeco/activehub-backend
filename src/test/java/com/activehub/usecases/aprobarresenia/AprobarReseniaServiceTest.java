package com.activehub.usecases.aprobarresenia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
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
class AprobarReseniaServiceTest {

    @Mock
    private ReseniaRepository reseniaRepository;
    @Mock
    private AuditService auditService;

    private AprobarReseniaService service;
    private UUID reseniaId;
    private Resenia resenia;

    @BeforeEach
    void setUp() {
        service = new AprobarReseniaService(reseniaRepository, auditService);

        reseniaId = UUID.randomUUID();
        resenia = new Resenia();
        resenia.setEnModeracion(true);
        ReflectionTestUtils.setField(resenia, "id", reseniaId);
    }

    @Test
    void aprobar_sacaDeModeracion() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        AprobarReseniaResponse response = service.aprobar(reseniaId, UUID.randomUUID());

        assertThat(response.enModeracion()).isFalse();
        assertThat(resenia.isEnModeracion()).isFalse();
    }

    @Test
    void aprobar_inexistente_lanzaNoEncontrado() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aprobar(reseniaId, UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
