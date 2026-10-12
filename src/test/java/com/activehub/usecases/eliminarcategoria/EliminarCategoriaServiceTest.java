package com.activehub.usecases.eliminarcategoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.CategoriaRepository;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.CategoriaEnUsoException;
import com.activehub.shared.error.NoEncontradoException;
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
class EliminarCategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private TipoActividadRepository tipoActividadRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private AuditService auditService;

    private EliminarCategoriaService service;
    private UUID categoriaId;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        service = new EliminarCategoriaService(
                categoriaRepository, tipoActividadRepository, actividadRepository, auditService);

        categoriaId = UUID.randomUUID();
        categoria = new Categoria();
        categoria.setNombre("Bienestar");
        ReflectionTestUtils.setField(categoria, "id", categoriaId);
    }

    private TipoActividad tipo(String nombre) {
        TipoActividad t = new TipoActividad();
        t.setNombre(nombre);
        ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
        return t;
    }

    @Test
    void eliminar_sinTiposAsociados_marcaBorrado() {
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(tipoActividadRepository.findByCategoriaIdAndDeletedFalseOrderByNombreAsc(categoriaId))
                .thenReturn(List.of());

        service.eliminar(categoriaId, UUID.randomUUID());

        assertThat(categoria.isDeleted()).isTrue();
        verify(categoriaRepository).save(categoria);
    }

    /**
     * El caso que motivó el cambio: un tipo sin actividades ya no bloquea, se da de baja junto
     * con la categoría. Antes había que borrarlo a mano antes de poder borrar la categoría.
     */
    @Test
    void eliminar_conTiposSinActividades_borraCategoriaYTipos() {
        TipoActividad yoga = tipo("Yoga");
        TipoActividad pilates = tipo("Pilates");
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(tipoActividadRepository.findByCategoriaIdAndDeletedFalseOrderByNombreAsc(categoriaId))
                .thenReturn(List.of(yoga, pilates));
        when(actividadRepository.existsByTipoActividadIdAndDeletedFalse(yoga.getId())).thenReturn(false);
        when(actividadRepository.existsByTipoActividadIdAndDeletedFalse(pilates.getId())).thenReturn(false);

        service.eliminar(categoriaId, UUID.randomUUID());

        assertThat(yoga.isDeleted()).isTrue();
        assertThat(pilates.isDeleted()).isTrue();
        assertThat(categoria.isDeleted()).isTrue();
        verify(tipoActividadRepository).save(yoga);
        verify(tipoActividadRepository).save(pilates);
        verify(categoriaRepository).save(categoria);
    }

    @Test
    void eliminar_conUnTipoConActividades_lanzaCategoriaEnUsoYNoBorraNada() {
        TipoActividad yoga = tipo("Yoga");
        TipoActividad pilates = tipo("Pilates");
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(tipoActividadRepository.findByCategoriaIdAndDeletedFalseOrderByNombreAsc(categoriaId))
                .thenReturn(List.of(yoga, pilates));
        when(actividadRepository.existsByTipoActividadIdAndDeletedFalse(yoga.getId())).thenReturn(false);
        when(actividadRepository.existsByTipoActividadIdAndDeletedFalse(pilates.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(categoriaId, UUID.randomUUID()))
                .isInstanceOf(CategoriaEnUsoException.class)
                .hasMessageContaining("Pilates");

        // Ni siquiera el tipo libre se da de baja: o se borra todo, o no se borra nada.
        assertThat(yoga.isDeleted()).isFalse();
        assertThat(categoria.isDeleted()).isFalse();
        verify(tipoActividadRepository, never()).save(yoga);
        verify(categoriaRepository, never()).save(categoria);
    }

    @Test
    void eliminar_inexistente_lanzaNoEncontrado() {
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(categoriaId, UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
