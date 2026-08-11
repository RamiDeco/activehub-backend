package com.activehub.usecases.crearclase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.SinPermisoException;
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
class CrearClaseServiceTest {

    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private PerfilInstructorRepository perfilInstructorRepository;
    @Mock
    private AuditService auditService;

    private CrearClaseService service;
    private UUID actividadId;
    private UUID instructorId;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        service = new CrearClaseService(actividadRepository, claseRepository, perfilInstructorRepository, auditService);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        actividadId = UUID.randomUUID();
        actividad = new Actividad();
        actividad.setInstructor(instructor);
        actividad.setCuposMax(20);
        ReflectionTestUtils.setField(actividad, "id", actividadId);
    }

    private PerfilInstructor perfilAprobado() {
        PerfilInstructor perfil = new PerfilInstructor(actividad.getInstructor(), "Running", 6, "d");
        perfil.setEstadoVerificacion(EstadoVerificacion.APROBADO);
        return perfil;
    }

    @Test
    void crear_sinCuposMax_usaElDeLaActividad() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfilAprobado()));
        when(claseRepository.save(any(Clase.class))).thenAnswer(inv -> {
            Clase c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });

        CrearClaseRequest request = new CrearClaseRequest(Instant.now().plus(3, ChronoUnit.DAYS), null);
        CrearClaseResponse response = service.crear(actividadId, request, instructorId);

        assertThat(response.cuposMax()).isEqualTo(20);
        assertThat(response.cuposOcupados()).isEqualTo(0);
        assertThat(response.estado()).isEqualTo("Programada");
    }

    @Test
    void crear_noDueño_lanzaSinPermiso() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        CrearClaseRequest request = new CrearClaseRequest(Instant.now().plus(3, ChronoUnit.DAYS), 10);
        assertThatThrownBy(() -> service.crear(actividadId, request, UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void crear_instructorNoAprobado_lanzaSinPermiso() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        PerfilInstructor perfilPendiente = new PerfilInstructor(actividad.getInstructor(), "Running", 6, "d");
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfilPendiente));

        CrearClaseRequest request = new CrearClaseRequest(Instant.now().plus(3, ChronoUnit.DAYS), 10);
        assertThatThrownBy(() -> service.crear(actividadId, request, instructorId))
                .isInstanceOf(SinPermisoException.class);
    }
}
