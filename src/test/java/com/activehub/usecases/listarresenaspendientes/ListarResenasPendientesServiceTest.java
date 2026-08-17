package com.activehub.usecases.listarresenaspendientes;

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
class ListarResenasPendientesServiceTest {

    @Mock
    private ReseniaRepository reseniaRepository;

    private ListarResenasPendientesService service;

    @BeforeEach
    void setUp() {
        service = new ListarResenasPendientesService(reseniaRepository);
    }

    @Test
    void listar_devuelveLasPendientes() {
        Actividad actividad = new Actividad();
        actividad.setNombre("Running");
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        Clase clase = new Clase();
        clase.setActividad(actividad);
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());

        Usuario alumno = new Usuario();
        alumno.setNombre("Bruno");
        alumno.setApellido("Diaz");
        ReflectionTestUtils.setField(alumno, "id", UUID.randomUUID());

        Resenia resenia = new Resenia();
        resenia.setClase(clase);
        resenia.setAlumno(alumno);
        resenia.setPuntaje(2);
        resenia.setComentario("No me gustó");
        ReflectionTestUtils.setField(resenia, "id", UUID.randomUUID());

        when(reseniaRepository.findPendientesConDetalle()).thenReturn(List.of(resenia));

        List<ListarResenasPendientesResponse> response = service.listar();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).actividadNombre()).isEqualTo("Running");
        assertThat(response.get(0).alumno().nombre()).isEqualTo("Bruno");
    }
}
