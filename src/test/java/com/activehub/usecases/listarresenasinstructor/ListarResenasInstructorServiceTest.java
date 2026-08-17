package com.activehub.usecases.listarresenasinstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
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
class ListarResenasInstructorServiceTest {

    @Mock
    private ReseniaRepository reseniaRepository;

    private ListarResenasInstructorService service;
    private UUID instructorId;

    @BeforeEach
    void setUp() {
        service = new ListarResenasInstructorService(reseniaRepository);
        instructorId = UUID.randomUUID();
    }

    @Test
    void listar_devuelveResenasDeSusActividades() {
        Actividad actividad = new Actividad();
        actividad.setNombre("Yoga Integral");
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        Clase clase = new Clase();
        clase.setActividad(actividad);
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());

        Usuario alumno = new Usuario();
        alumno.setNombre("Ana");
        alumno.setApellido("Lopez");
        ReflectionTestUtils.setField(alumno, "id", UUID.randomUUID());

        Resenia resenia = new Resenia();
        resenia.setClase(clase);
        resenia.setAlumno(alumno);
        resenia.setPuntaje(3);
        resenia.setComentario("Estuvo bien");
        resenia.setEnModeracion(false);
        ReflectionTestUtils.setField(resenia, "id", UUID.randomUUID());

        when(reseniaRepository.findByInstructorIdConDetalle(instructorId)).thenReturn(List.of(resenia));

        List<ListarResenasInstructorResponse> response = service.listar(instructorId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).actividadNombre()).isEqualTo("Yoga Integral");
        assertThat(response.get(0).alumno().nombre()).isEqualTo("Ana");
        assertThat(response.get(0).enModeracion()).isFalse();
    }
}
