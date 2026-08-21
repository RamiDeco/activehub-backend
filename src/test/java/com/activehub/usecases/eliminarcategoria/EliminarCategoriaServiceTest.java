package com.activehub.usecases.eliminarcategoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.CategoriaRepository;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.CategoriaEnUsoException;
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
class EliminarCategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private TipoActividadRepository tipoActividadRepository;
    @Mock
    private AuditService auditService;

    private EliminarCategoriaService service;
    private UUID categoriaId;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        service = new EliminarCategoriaService(categoriaRepository, tipoActividadRepository, auditService);

        categoriaId = UUID.randomUUID();
        categoria = new Categoria();
        categoria.setNombre("Bienestar");
        ReflectionTestUtils.setField(categoria, "id", categoriaId);
    }

    @Test
    void eliminar_sinTiposAsociados_marcaBorrado() {
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(tipoActividadRepository.existsByCategoriaIdAndDeletedFalse(categoriaId)).thenReturn(false);

        service.eliminar(categoriaId, UUID.randomUUID());

        assertThat(categoria.isDeleted()).isTrue();
        verify(categoriaRepository).save(categoria);
    }

    @Test
    void eliminar_conTiposAsociados_lanzaCategoriaEnUso() {
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(tipoActividadRepository.existsByCategoriaIdAndDeletedFalse(categoriaId)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(categoriaId, UUID.randomUUID()))
                .isInstanceOf(CategoriaEnUsoException.class);

        verify(categoriaRepository, never()).save(categoria);
    }

    @Test
    void eliminar_inexistente_lanzaNoEncontrado() {
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(categoriaId, UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
