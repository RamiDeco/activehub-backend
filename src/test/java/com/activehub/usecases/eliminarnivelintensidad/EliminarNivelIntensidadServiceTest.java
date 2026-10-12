package com.activehub.usecases.eliminarnivelintensidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.NivelIntensidadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NivelIntensidadEnUsoException;
import com.activehub.shared.error.NoEncontradoException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** E4Ad-HU05 criterios 6 y 7 (y RN-13: la baja es lógica). */
@ExtendWith(MockitoExtension.class)
class EliminarNivelIntensidadServiceTest {

    @Mock
    private NivelIntensidadRepository nivelIntensidadRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private AuditService auditService;

    private EliminarNivelIntensidadService service;
    private UUID nivelId;
    private NivelIntensidad nivel;

    @BeforeEach
    void setUp() {
        service = new EliminarNivelIntensidadService(nivelIntensidadRepository, actividadRepository, auditService);
        nivelId = UUID.randomUUID();
        nivel = new NivelIntensidad();
        nivel.setNombre("Física baja");
        nivel.setDescripcion("Bajo impacto.");
        ReflectionTestUtils.setField(nivel, "id", nivelId);
    }

    @Test
    void eliminar_sinActividadesAsociadas_haceBajaLogica() {
        when(nivelIntensidadRepository.findById(nivelId)).thenReturn(Optional.of(nivel));
        when(actividadRepository.existsByNivelIntensidadIdAndDeletedFalse(nivelId)).thenReturn(false);

        service.eliminar(nivelId, UUID.randomUUID());

        assertThat(nivel.isDeleted()).isTrue();
        verify(nivelIntensidadRepository).save(nivel);
        // RN-13: nunca un DELETE físico.
        verify(nivelIntensidadRepository, never()).delete(any());
    }

    @Test
    void eliminar_registraAuditoria() {
        when(nivelIntensidadRepository.findById(nivelId)).thenReturn(Optional.of(nivel));
        when(actividadRepository.existsByNivelIntensidadIdAndDeletedFalse(nivelId)).thenReturn(false);
        UUID actorId = UUID.randomUUID();

        service.eliminar(nivelId, actorId);

        verify(auditService).registrar(
                eq(actorId), eq(AuditAccion.NIVEL_INTENSIDAD_ELIMINADO), eq("NivelIntensidad"), eq(nivelId), eq(null));
    }

    @Test
    void eliminar_conActividadesAsociadas_lanzaEnUsoYNoBorra() {
        when(nivelIntensidadRepository.findById(nivelId)).thenReturn(Optional.of(nivel));
        when(actividadRepository.existsByNivelIntensidadIdAndDeletedFalse(nivelId)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(nivelId, UUID.randomUUID()))
                .isInstanceOf(NivelIntensidadEnUsoException.class)
                .hasMessageContaining("actividades asociadas");

        assertThat(nivel.isDeleted()).isFalse();
        verify(nivelIntensidadRepository, never()).save(any());
    }

    @Test
    void eliminar_nivelInexistente_lanzaNoEncontrado() {
        when(nivelIntensidadRepository.findById(nivelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(nivelId, UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
