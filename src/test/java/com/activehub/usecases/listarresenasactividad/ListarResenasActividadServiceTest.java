package com.activehub.usecases.listarresenasactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.error.NoEncontradoException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarResenasActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private ReseniaRepository reseniaRepository;

    private ListarResenasActividadService service;
    private UUID actividadId;

    @BeforeEach
    void setUp() {
        service = new ListarResenasActividadService(actividadRepository, reseniaRepository);
        actividadId = UUID.randomUUID();
    }

    @Test
    void listar_actividadExistente_devuelveVisibles() {
        when(actividadRepository.existsById(actividadId)).thenReturn(true);

        Usuario alumno = new Usuario();
        alumno.setNombre("Ana");
        alumno.setApellido("Lopez");
        ReflectionTestUtils.setField(alumno, "id", UUID.randomUUID());

        Clase clase = new Clase();
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());

        Resenia resenia = new Resenia();
        resenia.setClase(clase);
        resenia.setAlumno(alumno);
        resenia.setPuntaje(5);
        resenia.setComentario("Genial");
        ReflectionTestUtils.setField(resenia, "id", UUID.randomUUID());

        when(reseniaRepository.findVisiblesPorActividad(actividadId)).thenReturn(List.of(resenia));

        List<ListarResenasActividadResponse> response = service.listar(actividadId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).alumno().nombre()).isEqualTo("Ana");
        assertThat(response.get(0).puntaje()).isEqualTo(5);
    }

    @Test
    void listar_actividadInexistente_lanzaNoEncontrado() {
        when(actividadRepository.existsById(actividadId)).thenReturn(false);

        assertThatThrownBy(() -> service.listar(actividadId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
