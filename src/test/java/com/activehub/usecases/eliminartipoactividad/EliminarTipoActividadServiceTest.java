package com.activehub.usecases.eliminartipoactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.TipoActividadEnUsoException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EliminarTipoActividadServiceTest {

    @Mock
    private TipoActividadRepository tipoActividadRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private AuditService auditService;

    private EliminarTipoActividadService service;
    private UUID tipoId;
    private TipoActividad tipo;

    @BeforeEach
    void setUp() {
        service = new EliminarTipoActividadService(tipoActividadRepository, actividadRepository, auditService);

        tipoId = UUID.randomUUID();
        tipo = new TipoActividad();
        tipo.setNombre("Yoga");
        ReflectionTestUtils.setField(tipo, "id", tipoId);
    }

    @Test
    void eliminar_sinActividadesAsociadas_marcaBorrado() {
        when(tipoActividadRepository.findById(tipoId)).thenReturn(Optional.of(tipo));
        when(actividadRepository.existsByTipoActividadIdAndDeletedFalse(tipoId)).thenReturn(false);

        service.eliminar(tipoId, UUID.randomUUID());

        assertThat(tipo.isDeleted()).isTrue();
        verify(tipoActividadRepository).save(tipo);
    }

    @Test
    void eliminar_conActividadesAsociadas_lanzaTipoActividadEnUso() {
        when(tipoActividadRepository.findById(tipoId)).thenReturn(Optional.of(tipo));
        when(actividadRepository.existsByTipoActividadIdAndDeletedFalse(tipoId)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(tipoId, UUID.randomUUID()))
                .isInstanceOf(TipoActividadEnUsoException.class);

        verify(tipoActividadRepository, org.mockito.Mockito.never()).save(tipo);
    }

    @Test
    void eliminar_inexistente_lanzaNoEncontrado() {
        when(tipoActividadRepository.findById(tipoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(tipoId, UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
