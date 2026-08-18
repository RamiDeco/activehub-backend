package com.activehub.usecases.listarclasesinstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarClasesInstructorServiceTest {

    @Mock
    private ClaseRepository claseRepository;

    private ListarClasesInstructorService service;
    private UUID instructorId;

    @BeforeEach
    void setUp() {
        service = new ListarClasesInstructorService(claseRepository);
        instructorId = UUID.randomUUID();
    }

    @Test
    void listar_devuelveClasesConDetalleDeActividad() {
        Actividad actividad = new Actividad();
        actividad.setNombre("Yoga");
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        Clase clase = new Clase();
        clase.setActividad(actividad);
        clase.setFechaHora(Instant.parse("2026-09-01T10:00:00Z"));
        clase.setEstado(EstadoClase.Programada);
        clase.setCuposMax(10);
        clase.setCuposOcupados(3);
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());

        when(claseRepository.findByInstructorIdConDetalle(instructorId)).thenReturn(List.of(clase));

        List<ListarClasesInstructorResponse> resultado = service.listar(instructorId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).actividadNombre()).isEqualTo("Yoga");
        assertThat(resultado.get(0).estado()).isEqualTo("Programada");
        assertThat(resultado.get(0).cuposOcupados()).isEqualTo(3);
    }

    @Test
    void listar_sinClases_devuelveListaVacia() {
        when(claseRepository.findByInstructorIdConDetalle(instructorId)).thenReturn(List.of());

        assertThat(service.listar(instructorId)).isEmpty();
    }
}
