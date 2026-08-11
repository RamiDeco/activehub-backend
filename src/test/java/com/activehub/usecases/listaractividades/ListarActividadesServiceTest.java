package com.activehub.usecases.listaractividades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.usuario.Usuario;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarActividadesServiceTest {

    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private ClaseRepository claseRepository;

    private ListarActividadesService service;

    private Actividad actividad(String nombre, BigDecimal precio, BigDecimal rating) {
        Categoria categoria = new Categoria();
        categoria.setNombre("Bienestar");
        ReflectionTestUtils.setField(categoria, "id", UUID.randomUUID());

        TipoActividad tipo = new TipoActividad();
        tipo.setNombre("Yoga");
        tipo.setCategoria(categoria);
        ReflectionTestUtils.setField(tipo, "id", UUID.randomUUID());

        Usuario instructor = new Usuario();
        instructor.setNombre("Carla");
        instructor.setApellido("Nuñez");
        ReflectionTestUtils.setField(instructor, "id", UUID.randomUUID());

        Actividad a = new Actividad();
        a.setNombre(nombre);
        a.setDescripcion("desc");
        a.setTipoActividad(tipo);
        a.setNivelIntensidad(NivelIntensidad.FISICA_BAJA);
        a.setInstructor(instructor);
        a.setPrecio(precio);
        a.setUbicacion("Mendoza");
        a.setPhotoTint("gradient");
        a.setRating(rating);
        a.setCuposMax(10);
        ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
        return a;
    }

    @BeforeEach
    void setUp() {
        service = new ListarActividadesService(actividadRepository, claseRepository);
    }

    @Test
    void listar_sinFiltros_devuelveTodasOrdenadasPorNombre() {
        Actividad a1 = actividad("Yoga", new BigDecimal("3000"), new BigDecimal("4.5"));
        Actividad a2 = actividad("Pilates", new BigDecimal("4000"), new BigDecimal("4.8"));
        when(actividadRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Sort.class)))
                .thenReturn(List.of(a1, a2));
        when(claseRepository.findByActividadIdInAndEstadoNotInOrderByFechaHoraAsc(any(), any()))
                .thenReturn(List.of());

        List<ListarActividadesResponse> resultado = service.listar(null, null, null, null, null, false, null, null);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).proximaClase()).isNull();
    }

    @Test
    void listar_soloConCupos_excluyeActividadesSinCupos() {
        Actividad a1 = actividad("Yoga", new BigDecimal("3000"), new BigDecimal("4.5"));
        Actividad a2 = actividad("Pilates", new BigDecimal("4000"), new BigDecimal("4.8"));
        when(actividadRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Sort.class)))
                .thenReturn(List.of(a1, a2));

        Clase claseSinCupos = new Clase();
        claseSinCupos.setActividad(a1);
        claseSinCupos.setFechaHora(Instant.now().plus(2, ChronoUnit.DAYS));
        claseSinCupos.setEstado(EstadoClase.Programada);
        claseSinCupos.setCuposMax(10);
        claseSinCupos.setCuposOcupados(10);
        ReflectionTestUtils.setField(claseSinCupos, "id", UUID.randomUUID());

        when(claseRepository.findByActividadIdInAndEstadoNotInOrderByFechaHoraAsc(any(), any()))
                .thenReturn(List.of(claseSinCupos));

        List<ListarActividadesResponse> resultado = service.listar(null, null, null, null, null, true, null, null);

        assertThat(resultado).extracting(ListarActividadesResponse::nombre).containsExactly("Pilates");
    }

    @Test
    void listar_sortPrecioDesc_pasaSortCorrectoAlRepositorio() {
        when(actividadRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Sort.class)))
                .thenReturn(List.of());

        service.listar(null, null, null, null, null, false, null, "precio_desc");

        var sortCaptor = org.mockito.ArgumentCaptor.forClass(Sort.class);
        org.mockito.Mockito.verify(actividadRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), sortCaptor.capture());
        assertThat(sortCaptor.getValue().getOrderFor("precio").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
