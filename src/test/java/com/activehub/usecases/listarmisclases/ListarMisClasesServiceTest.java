package com.activehub.usecases.listarmisclases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.usuario.Usuario;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarMisClasesServiceTest {

    @Mock private ClaseRepository claseRepository;

    @InjectMocks private ListarMisClasesService service;

    private Clase clase(String actividadNombre, String ubicacion, EstadoClase estado, int max, int ocupados) {
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", UUID.randomUUID());

        Actividad actividad = new Actividad();
        actividad.setNombre(actividadNombre);
        actividad.setUbicacion(ubicacion);
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        Clase c = new Clase();
        c.setActividad(actividad);
        c.setEstado(estado);
        c.setFechaHora(Instant.parse("2026-06-01T10:00:00Z"));
        c.setCuposMax(max);
        c.setCuposOcupados(ocupados);
        ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
        return c;
    }

    @Test
    void listar_devuelveLosDatosQueNecesitaElPanel() {
        UUID instructorId = UUID.randomUUID();
        Clase c = clase("Yoga", "Parque San Martín", EstadoClase.Programada, 20, 8);
        when(claseRepository.findByInstructorIdConDetalle(instructorId)).thenReturn(List.of(c));

        List<ListarMisClasesResponse> resultado = service.listar(instructorId);

        assertThat(resultado).hasSize(1);
        var r = resultado.get(0);
        assertThat(r.actividadNombre()).isEqualTo("Yoga");
        assertThat(r.actividadUbicacion()).isEqualTo("Parque San Martín");
        assertThat(r.estado()).isEqualTo("Programada");
        assertThat(r.cuposMax()).isEqualTo(20);
        assertThat(r.cuposOcupados()).isEqualTo(8);
    }

    @Test
    void listar_incluyeFinalizadasYCanceladas_paraQueElInstructorVeaSuHistorial() {
        UUID instructorId = UUID.randomUUID();
        when(claseRepository.findByInstructorIdConDetalle(instructorId)).thenReturn(List.of(
                clase("Running", "Costanera", EstadoClase.Finalizada, 10, 10),
                clase("Trekking", "Cerro Arco", EstadoClase.Cancelada, 12, 3)));

        List<ListarMisClasesResponse> resultado = service.listar(instructorId);

        assertThat(resultado).extracting(ListarMisClasesResponse::estado)
                .containsExactlyInAnyOrder("Finalizada", "Cancelada");
    }

    @Test
    void listar_sinClases_devuelveListaVacia() {
        UUID instructorId = UUID.randomUUID();
        when(claseRepository.findByInstructorIdConDetalle(instructorId)).thenReturn(List.of());

        assertThat(service.listar(instructorId)).isEmpty();
    }
}
