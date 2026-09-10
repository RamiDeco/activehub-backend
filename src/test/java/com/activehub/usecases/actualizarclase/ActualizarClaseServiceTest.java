package com.activehub.usecases.actualizarclase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.security.InstructorVerificadoGuard;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ActualizarClaseServiceTest {

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private AuditService auditService;

    @org.mockito.Mock private InstructorVerificadoGuard instructorVerificadoGuard;


    private ActualizarClaseService service;
    private UUID claseId;
    private UUID instructorId;
    private Clase clase;

    @BeforeEach
    void setUp() {
        service = new ActualizarClaseService(claseRepository, auditService, instructorVerificadoGuard);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        claseId = UUID.randomUUID();
        clase = new Clase();
        clase.setActividad(actividad);
        clase.setFechaHora(Instant.now().plus(1, ChronoUnit.DAYS));
        clase.setHoraFin(Instant.now().plus(1, ChronoUnit.DAYS).plusSeconds(3600));
        clase.setEstado(EstadoClase.Programada);
        clase.setCuposMax(10);
        clase.setCuposOcupados(6);
        ReflectionTestUtils.setField(clase, "id", claseId);

        nuevoInicio = Instant.now().plus(2, ChronoUnit.DAYS);
        nuevoFin = nuevoInicio.plusSeconds(3600);
    }

    private Instant nuevoInicio;
    private Instant nuevoFin;

    @Test
    void actualizar_cuposMaxValido_actualiza() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        ActualizarClaseRequest request = new ActualizarClaseRequest(nuevoInicio, nuevoFin, 12);
        ActualizarClaseResponse response = service.actualizar(claseId, request, instructorId);

        assertThat(response.cuposMax()).isEqualTo(12);
        assertThat(response.horaFin()).isEqualTo(nuevoFin);
    }

    @Test
    void actualizar_cuposMaxMenorAOcupados_lanzaValidacion() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        ActualizarClaseRequest request = new ActualizarClaseRequest(nuevoInicio, nuevoFin, 3);
        assertThatThrownBy(() -> service.actualizar(claseId, request, instructorId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void actualizar_horaFinAnteriorAlInicio_lanzaValidacion() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        ActualizarClaseRequest request = new ActualizarClaseRequest(nuevoInicio, nuevoInicio.minusSeconds(1), 12);
        assertThatThrownBy(() -> service.actualizar(claseId, request, instructorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("posterior a la hora de inicio");
    }

    @Test
    void actualizar_solapaConOtraClase_lanzaValidacion() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        // Se excluye la propia clase del chequeo: si no, editar el cupo sin tocar el horario
        // fallaría contra sí misma.
        when(claseRepository.existeSolapamiento(any(), eq(nuevoInicio), eq(nuevoFin), eq(claseId))).thenReturn(true);

        ActualizarClaseRequest request = new ActualizarClaseRequest(nuevoInicio, nuevoFin, 12);
        assertThatThrownBy(() -> service.actualizar(claseId, request, instructorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("Ya existe una clase en ese horario");
    }

    @Test
    void actualizar_noDueño_lanzaSinPermiso() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        ActualizarClaseRequest request = new ActualizarClaseRequest(nuevoInicio, nuevoFin, 12);
        assertThatThrownBy(() -> service.actualizar(claseId, request, UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);
    }
}
