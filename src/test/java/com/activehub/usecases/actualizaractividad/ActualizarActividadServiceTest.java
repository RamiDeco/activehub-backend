package com.activehub.usecases.actualizaractividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ActualizarActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private TipoActividadRepository tipoActividadRepository;
    @Mock
    private PerfilInstructorRepository perfilInstructorRepository;
    @Mock
    private AuditService auditService;

    private ActualizarActividadService service;
    private UUID actividadId;
    private UUID instructorId;
    private Actividad actividad;
    private TipoActividad tipo;

    @BeforeEach
    void setUp() {
        service = new ActualizarActividadService(actividadRepository, tipoActividadRepository, perfilInstructorRepository, auditService);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        instructor.setNombre("Mateo");
        instructor.setApellido("Herrera");
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Categoria categoria = new Categoria();
        categoria.setNombre("Aventura");
        ReflectionTestUtils.setField(categoria, "id", UUID.randomUUID());

        tipo = new TipoActividad();
        tipo.setNombre("Aventura");
        tipo.setCategoria(categoria);
        ReflectionTestUtils.setField(tipo, "id", UUID.randomUUID());

        actividadId = UUID.randomUUID();
        actividad = new Actividad();
        actividad.setNombre("Running");
        actividad.setDescripcion("desc");
        actividad.setTipoActividad(tipo);
        actividad.setNivelIntensidad(NivelIntensidad.FISICA_ALTA);
        actividad.setInstructor(instructor);
        actividad.setPrecio(new BigDecimal("4000"));
        actividad.setUbicacion("Mendoza");
        actividad.setPhotoTint("gradient");
        actividad.setCuposMax(10);
        ReflectionTestUtils.setField(actividad, "id", actividadId);
    }

    private ActualizarActividadRequest requestValido() {
        return new ActualizarActividadRequest(
                "Running actualizado", "nueva desc", tipo.getId(), "Física media",
                new BigDecimal("5000"), "Nueva ubicacion", "gradient2", 20);
    }

    @Test
    void actualizar_dueñoAprobado_actualizaActividad() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        PerfilInstructor perfil = new PerfilInstructor(actividad.getInstructor(), "Running", 6, "d");
        perfil.setEstadoVerificacion(EstadoVerificacion.APROBADO);
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfil));
        when(tipoActividadRepository.findById(tipo.getId())).thenReturn(Optional.of(tipo));

        ActualizarActividadResponse response = service.actualizar(actividadId, requestValido(), instructorId);

        assertThat(response.nombre()).isEqualTo("Running actualizado");
        assertThat(response.cuposMax()).isEqualTo(20);
    }

    @Test
    void actualizar_noDueño_lanzaSinPermiso() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        UUID otroInstructor = UUID.randomUUID();
        assertThatThrownBy(() -> service.actualizar(actividadId, requestValido(), otroInstructor))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void actualizar_actividadInexistente_lanzaNoEncontrado() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(actividadId, requestValido(), instructorId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
