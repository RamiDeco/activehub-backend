package com.activehub.usecases.listartiposactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * El filtro cascada Categoría → Tipo de actividad de Explorar (E3A-HU02) depende de que el
 * parámetro opcional cambie de consulta: con categoría, sólo los tipos de esa categoría.
 */
@ExtendWith(MockitoExtension.class)
class ListarTiposActividadServiceTest {

    @Mock
    private TipoActividadRepository tipoActividadRepository;

    private ListarTiposActividadService service;
    private Categoria aventura;

    @BeforeEach
    void setUp() {
        service = new ListarTiposActividadService(tipoActividadRepository);
        aventura = new Categoria();
        aventura.setNombre("Aventura");
        ReflectionTestUtils.setField(aventura, "id", UUID.randomUUID());
    }

    private TipoActividad tipo(String nombre) {
        TipoActividad t = new TipoActividad();
        t.setNombre(nombre);
        t.setCategoria(aventura);
        ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
        return t;
    }

    @Test
    void listar_sinCategoria_devuelveTodosLosTipos() {
        when(tipoActividadRepository.findAllByDeletedFalseOrderByNombreAsc())
                .thenReturn(List.of(tipo("Escalada"), tipo("Trekking")));

        assertThat(service.listar(null))
                .extracting(ListarTiposActividadResponse::nombre)
                .containsExactly("Escalada", "Trekking");

        verify(tipoActividadRepository, never()).findByCategoriaIdAndDeletedFalseOrderByNombreAsc(any());
    }

    @Test
    void listar_conCategoria_consultaSoloEsaCategoria() {
        when(tipoActividadRepository.findByCategoriaIdAndDeletedFalseOrderByNombreAsc(aventura.getId()))
                .thenReturn(List.of(tipo("Trekking")));

        assertThat(service.listar(aventura.getId()))
                .extracting(ListarTiposActividadResponse::nombre)
                .containsExactly("Trekking");

        verify(tipoActividadRepository, never()).findAllByDeletedFalseOrderByNombreAsc();
    }

    @Test
    void listar_devuelveLaCategoriaDeCadaTipo() {
        when(tipoActividadRepository.findAllByDeletedFalseOrderByNombreAsc()).thenReturn(List.of(tipo("Trekking")));

        assertThat(service.listar(null).get(0).categoriaId()).isEqualTo(aventura.getId());
    }

    @Test
    void listar_sinResultados_devuelveListaVacia() {
        when(tipoActividadRepository.findAllByDeletedFalseOrderByNombreAsc()).thenReturn(List.of());

        assertThat(service.listar(null)).isEmpty();
    }

    private static UUID any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
