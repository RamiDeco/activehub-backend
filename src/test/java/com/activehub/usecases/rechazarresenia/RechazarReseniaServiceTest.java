package com.activehub.usecases.rechazarresenia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Clase;
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
    private ActividadRepository actividadRepository;
    @Mock
    private AuditService auditService;

    private RechazarReseniaService service;
    private UUID reseniaId;
    private UUID actividadId;
    private Resenia resenia;

    @BeforeEach
    void setUp() {
        service = new RechazarReseniaService(reseniaRepository, actividadRepository, auditService);

        actividadId = UUID.randomUUID();
        Actividad actividad = new Actividad();
        ReflectionTestUtils.setField(actividad, "id", actividadId);

        Clase clase = new Clase();
        clase.setActividad(actividad);

        reseniaId = UUID.randomUUID();
        resenia = new Resenia();
        resenia.setClase(clase);
        ReflectionTestUtils.setField(resenia, "id", reseniaId);
    }

    @Test
    void rechazar_marcaBorradaYRecalculaRating() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        service.rechazar(reseniaId, UUID.randomUUID());

        assertThat(resenia.isDeleted()).isTrue();
        verify(actividadRepository).recalcularRating(actividadId);
    }

    @Test
    void rechazar_inexistente_lanzaNoEncontrado() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rechazar(reseniaId, UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
