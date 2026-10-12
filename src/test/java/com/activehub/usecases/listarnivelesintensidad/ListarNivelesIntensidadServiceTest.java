package com.activehub.usecases.listarnivelesintensidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.NivelIntensidadRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** E4Ad-HU05 criterio 1: la tabla muestra Nivel, Descripción y cuántas actividades lo usan. */
@ExtendWith(MockitoExtension.class)
class ListarNivelesIntensidadServiceTest {

    @Mock
    private NivelIntensidadRepository nivelIntensidadRepository;
    @Mock
    private ActividadRepository actividadRepository;

    private ListarNivelesIntensidadService service;

    @BeforeEach
    void setUp() {
        service = new ListarNivelesIntensidadService(nivelIntensidadRepository, actividadRepository);
    }

    private static NivelIntensidad nivel(UUID id, String nombre) {
        NivelIntensidad n = new NivelIntensidad();
        n.setNombre(nombre);
        n.setDescripcion("desc de " + nombre);
        ReflectionTestUtils.setField(n, "id", id);
        return n;
    }

    @Test
    void listar_devuelveCadaNivelConSuConteoDeActividades() {
        UUID bajaId = UUID.randomUUID();
        UUID altaId = UUID.randomUUID();
        when(nivelIntensidadRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(nivel(altaId, "Física alta"), nivel(bajaId, "Física baja")));
        when(actividadRepository.countByNivelIntensidadIdAndDeletedFalse(altaId)).thenReturn(3L);
        when(actividadRepository.countByNivelIntensidadIdAndDeletedFalse(bajaId)).thenReturn(0L);

        List<ListarNivelesIntensidadResponse> resultado = service.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).nombre()).isEqualTo("Física alta");
        assertThat(resultado.get(0).descripcion()).isEqualTo("desc de Física alta");
        assertThat(resultado.get(0).actividades()).isEqualTo(3);
        assertThat(resultado.get(1).nombre()).isEqualTo("Física baja");
        assertThat(resultado.get(1).actividades()).isZero();
    }

    @Test
    void listar_sinNiveles_devuelveListaVacia() {
        when(nivelIntensidadRepository.findAllByOrderByNombreAsc()).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }
}
