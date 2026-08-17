package com.activehub.usecases.marcarasistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MarcarAsistenciaServiceTest {

    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private AuditService auditService;

    private MarcarAsistenciaService service;
    private UUID instructorId;
    private UUID inscripcionId;
    private Inscripcion inscripcion;

    @BeforeEach
    void setUp() {
        service = new MarcarAsistenciaService(inscripcionRepository, auditService);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setInstructor(instructor);

        Clase clase = new Clase();
        clase.setActividad(actividad);

        inscripcionId = UUID.randomUUID();
        inscripcion = new Inscripcion();
        inscripcion.setClase(clase);
        inscripcion.setEstado(EstadoInscripcion.INSCRIPTO);
        ReflectionTestUtils.setField(inscripcion, "id", inscripcionId);
    }

    @Test
    void marcar_presente_true() {
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        MarcarAsistenciaResponse response = service.marcar(inscripcionId, new MarcarAsistenciaRequest(true), instructorId);

        assertThat(response.presente()).isTrue();
        assertThat(inscripcion.getPresente()).isTrue();
    }

    @Test
    void marcar_presente_false() {
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        MarcarAsistenciaResponse response = service.marcar(inscripcionId, new MarcarAsistenciaRequest(false), instructorId);

        assertThat(response.presente()).isFalse();
    }

    @Test
    void marcar_noDueño_lanzaSinPermiso() {
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        assertThatThrownBy(() -> service.marcar(inscripcionId, new MarcarAsistenciaRequest(true), UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void marcar_inscripcionCancelada_lanzaValidacion() {
        inscripcion.setEstado(EstadoInscripcion.CANCELADA);
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.of(inscripcion));

        assertThatThrownBy(() -> service.marcar(inscripcionId, new MarcarAsistenciaRequest(true), instructorId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void marcar_inexistente_lanzaNoEncontrado() {
        when(inscripcionRepository.findById(inscripcionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcar(inscripcionId, new MarcarAsistenciaRequest(true), instructorId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
