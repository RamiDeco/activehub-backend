package com.activehub.usecases.listarmisresenas;

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
class ListarMisResenasServiceTest {

    @Mock
    private ReseniaRepository reseniaRepository;

    private ListarMisResenasService service;
    private UUID alumnoId;

    @BeforeEach
    void setUp() {
        service = new ListarMisResenasService(reseniaRepository);
        alumnoId = UUID.randomUUID();
    }

    @Test
    void listar_devuelvePropiasConDetalle() {
        Usuario instructor = new Usuario();
        instructor.setNombre("Carla");
        instructor.setApellido("Nuñez");

        Actividad actividad = new Actividad();
        actividad.setNombre("Yoga Integral");
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        Clase clase = new Clase();
        clase.setActividad(actividad);
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());

        Resenia resenia = new Resenia();
        resenia.setClase(clase);
        resenia.setPuntaje(4);
        resenia.setComentario("Buena clase");
        resenia.setEnModeracion(true);
        resenia.setRespuestaInstructor("¡Gracias por venir!");
        ReflectionTestUtils.setField(resenia, "id", UUID.randomUUID());

        when(reseniaRepository.findByAlumnoIdConDetalle(alumnoId)).thenReturn(List.of(resenia));

        List<ListarMisResenasResponse> response = service.listar(alumnoId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).actividadNombre()).isEqualTo("Yoga Integral");
        assertThat(response.get(0).instructorNombre()).isEqualTo("Carla Nuñez");
        assertThat(response.get(0).enModeracion()).isTrue();
        // La respuesta del instructor viaja al alumno: es lo que abre la notificación
        // RESENIA_RESPONDIDA, que hasta ahora lo dejaba en una lista donde no estaba.
        assertThat(response.get(0).respuestaInstructor()).isEqualTo("¡Gracias por venir!");
        assertThat(response.get(0).oculta()).isFalse();
    }
}
