package com.activehub.usecases.listarmisfavoritos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.favorito.ActividadFavorita;
import com.activehub.domain.favorito.FavoritoRepository;
import com.activehub.domain.usuario.Usuario;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarMisFavoritosServiceTest {

    @Mock
    private FavoritoRepository favoritoRepository;

    private ListarMisFavoritosService service;
    private UUID alumnoId;

    @BeforeEach
    void setUp() {
        service = new ListarMisFavoritosService(favoritoRepository);
        alumnoId = UUID.randomUUID();
    }

    @Test
    void listar_devuelveIdsDeActividades() {
        UUID actividadId = UUID.randomUUID();
        Actividad actividad = new Actividad();
        ReflectionTestUtils.setField(actividad, "id", actividadId);
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        when(favoritoRepository.findByUsuarioId(alumnoId)).thenReturn(List.of(new ActividadFavorita(alumno, actividad)));

        List<UUID> resultado = service.listar(alumnoId);

        assertThat(resultado).containsExactly(actividadId);
    }

    @Test
    void listar_sinFavoritos_devuelveListaVacia() {
        when(favoritoRepository.findByUsuarioId(alumnoId)).thenReturn(List.of());

        assertThat(service.listar(alumnoId)).isEmpty();
    }
}
