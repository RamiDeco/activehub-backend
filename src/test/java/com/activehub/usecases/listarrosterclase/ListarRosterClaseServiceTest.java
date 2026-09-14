package com.activehub.usecases.listarrosterclase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import java.util.List;
import java.util.Optional;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarRosterClaseServiceTest {

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;

    @org.mockito.Mock private InstructorVerificadoGuard instructorVerificadoGuard;


    private ListarRosterClaseService service;
    private UUID instructorId;
    private UUID claseId;
    private Clase clase;

    @BeforeEach
    void setUp() {
        service = new ListarRosterClaseService(claseRepository, inscripcionRepository, instructorVerificadoGuard);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setInstructor(instructor);

        claseId = UUID.randomUUID();
        clase = new Clase();
        clase.setActividad(actividad);
        clase.setCuposMax(10);
        clase.setCuposOcupados(3);
        ReflectionTestUtils.setField(clase, "id", claseId);
    }

    private Inscripcion inscripcionCon(EstadoInscripcion estado) {
        Usuario alumno = new Usuario();
        alumno.setNombre("Ana");
        alumno.setApellido("Lopez");
        ReflectionTestUtils.setField(alumno, "id", UUID.randomUUID());

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setClase(clase);
        inscripcion.setAlumno(alumno);
        inscripcion.setEstado(estado);
        ReflectionTestUtils.setField(inscripcion, "id", UUID.randomUUID());
        return inscripcion;
    }

    @Test
    void listar_incluyeCantidadDePreinscriptosSinListarlos() {
        Inscripcion inscripto = inscripcionCon(EstadoInscripcion.INSCRIPTO);
        Inscripcion pendiente = inscripcionCon(EstadoInscripcion.PAGO_PENDIENTE);

        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoInOrderByCreatedAtAsc(claseId, List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.PAGO_PENDIENTE)))
                .thenReturn(List.of(inscripto, pendiente));
        when(inscripcionRepository.countByClaseIdAndEstado(claseId, EstadoInscripcion.PRE_INSCRIPCION)).thenReturn(4L);

        ListarRosterClaseResponse response = service.listar(claseId, instructorId);

        assertThat(response.cantidadInscripto()).isEqualTo(1);
        assertThat(response.cantidadPagoPendiente()).isEqualTo(1);
        assertThat(response.cantidadPreInscripcion()).isEqualTo(4);
        assertThat(response.alumnos()).hasSize(2);
        assertThat(response.cuposLibres()).isEqualTo(7);
    }

    @Test
    void listar_noDueño_lanzaSinPermiso() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.listar(claseId, UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void listar_inexistente_lanzaNoEncontrado() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(claseId, instructorId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
