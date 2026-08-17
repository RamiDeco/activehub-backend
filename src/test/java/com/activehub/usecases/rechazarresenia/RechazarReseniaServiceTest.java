package com.activehub.usecases.rechazarresenia;

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
class RechazarReseniaServiceTest {

    @Mock
    private ReseniaRepository reseniaRepository;
    @Mock
    private AuditService auditService;

    private RechazarReseniaService service;
    private UUID reseniaId;
    private Resenia resenia;

    @BeforeEach
    void setUp() {
        service = new RechazarReseniaService(reseniaRepository, auditService);

        reseniaId = UUID.randomUUID();
        resenia = new Resenia();
        ReflectionTestUtils.setField(resenia, "id", reseniaId);
    }

    @Test
    void rechazar_marcaBorrada() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        service.rechazar(reseniaId, UUID.randomUUID());

        assertThat(resenia.isDeleted()).isTrue();
    }

    @Test
    void rechazar_inexistente_lanzaNoEncontrado() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rechazar(reseniaId, UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
