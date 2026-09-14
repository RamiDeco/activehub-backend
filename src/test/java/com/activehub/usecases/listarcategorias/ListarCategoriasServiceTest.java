package com.activehub.usecases.listarcategorias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.CategoriaRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarCategoriasServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    private ListarCategoriasService service;

    @BeforeEach
    void setUp() {
        service = new ListarCategoriasService(categoriaRepository);
    }

    private static Categoria categoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
        return c;
    }

    @Test
    void listar_respetaElOrdenAlfabeticoDelRepositorio() {
        when(categoriaRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(categoria("Aventura"), categoria("Bienestar"), categoria("Deportivas")));

        assertThat(service.listar())
                .extracting(ListarCategoriasResponse::nombre)
                .containsExactly("Aventura", "Bienestar", "Deportivas");
    }

    @Test
    void listar_devuelveElIdDeCadaCategoria() {
        Categoria aventura = categoria("Aventura");
        when(categoriaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(aventura));

        assertThat(service.listar().get(0).id()).isEqualTo(aventura.getId());
    }

    @Test
    void listar_sinCategorias_devuelveListaVacia() {
        when(categoriaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }
}
