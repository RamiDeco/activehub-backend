package com.activehub.usecases.obteneractividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadImagen;
import com.activehub.domain.actividad.ActividadImagenRepository;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.error.NoEncontradoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class ObtenerActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private ActividadImagenRepository actividadImagenRepository;

    private ObtenerActividadService service;
    private UUID actividadId;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        service = new ObtenerActividadService(
                actividadRepository, claseRepository, inscripcionRepository, actividadImagenRepository);

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

        actividadId = UUID.randomUUID();
        actividad = new Actividad();
        actividad.setNombre("Yoga Integral");
        actividad.setDescripcion("desc");
        actividad.setTipoActividad(tipo);
        actividad.setNivelIntensidad(nivel("Física baja"));
        actividad.setInstructor(instructor);
        actividad.setPrecio(new BigDecimal("3600"));
        actividad.setUbicacion("Mendoza");
        actividad.setPhotoTint("gradient");
        actividad.setDuracionMin(60);
        ReflectionTestUtils.setField(actividad, "id", actividadId);
    }

    @Test
    void obtener_actividadExistente_devuelveJoinsYClasesOrdenadas() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        Clase clase1 = new Clase();
        clase1.setActividad(actividad);
        clase1.setFechaHora(Instant.now().plus(1, ChronoUnit.DAYS));
        clase1.setEstado(EstadoClase.Programada);
        clase1.setHoraFin(clase1.getFechaHora().plus(1, ChronoUnit.HOURS));
        clase1.setCuposMax(16);
        clase1.setCuposOcupados(2);
        ReflectionTestUtils.setField(clase1, "id", UUID.randomUUID());

        when(claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(any(), any()))
                .thenReturn(List.of(clase1));
        when(inscripcionRepository.countByClaseIdAndEstado(clase1.getId(), EstadoInscripcion.PRE_INSCRIPCION))
                .thenReturn(3L);
        when(actividadImagenRepository.findByActividadIdOrderByOrdenAsc(actividadId)).thenReturn(List.of());

        ObtenerActividadResponse response = service.obtener(actividadId);

        assertThat(response.nombre()).isEqualTo("Yoga Integral");
        assertThat(response.tipoActividad().nombre()).isEqualTo("Yoga");
        assertThat(response.categoria().nombre()).isEqualTo("Bienestar");
        assertThat(response.instructor().nombre()).isEqualTo("Carla");
        assertThat(response.duracionMin()).isEqualTo(60);
        assertThat(response.clases()).hasSize(1);
        assertThat(response.clases().get(0).cuposOcupados()).isEqualTo(2);
        assertThat(response.clases().get(0).horaFin()).isEqualTo(clase1.getHoraFin());
        assertThat(response.clases().get(0).cantidadPreInscripcion()).isEqualTo(3);
    }

    @Test
    void obtener_devuelveLaGaleriaDeImagenes() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(any(), any())).thenReturn(List.of());

        ActividadImagen imagen = new ActividadImagen();
        UUID imagenId = UUID.randomUUID();
        ReflectionTestUtils.setField(imagen, "id", imagenId);
        when(actividadImagenRepository.findByActividadIdOrderByOrdenAsc(actividadId)).thenReturn(List.of(imagen));

        assertThat(service.obtener(actividadId).imagenes()).containsExactly(imagenId);
    }

    @Test
    void obtener_inexistente_lanzaNoEncontrado() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(actividadId))
                .isInstanceOf(NoEncontradoException.class);
    }

    private static NivelIntensidad nivel(String nombre) {
        NivelIntensidad n = new NivelIntensidad();
        n.setNombre(nombre);
        n.setDescripcion("desc");
        ReflectionTestUtils.setField(n, "id", UUID.randomUUID());
        return n;
    }
}
