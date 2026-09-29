package com.activehub.usecases.crearactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.NivelIntensidadRepository;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.security.PenalizacionVigenteGuard;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
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
class CrearActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private TipoActividadRepository tipoActividadRepository;
    @Mock
    private NivelIntensidadRepository nivelIntensidadRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilInstructorRepository perfilInstructorRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private PenalizacionVigenteGuard penalizacionVigenteGuard;

    private CrearActividadService service;
    private UUID instructorId;
    private Usuario instructor;
    private TipoActividad tipo;
    private NivelIntensidad nivel;

    @BeforeEach
    void setUp() {
        service = new CrearActividadService(
                actividadRepository, tipoActividadRepository, nivelIntensidadRepository, usuarioRepository,
                perfilInstructorRepository, auditService, penalizacionVigenteGuard);

        instructorId = UUID.randomUUID();
        instructor = new Usuario();
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

        nivel = new NivelIntensidad();
        nivel.setNombre("Física alta");
        nivel.setDescripcion("desc");
        ReflectionTestUtils.setField(nivel, "id", UUID.randomUUID());
    }

    private CrearActividadRequest requestValido() {
        return new CrearActividadRequest(
                "Running en grupo", "Salidas grupales", tipo.getId(), nivel.getId(),
                new BigDecimal("4500"), "Parque Gral. San Martín", "gradient", 16, null, null);
    }

    private CrearActividadRequest requestConCoordenadas(Double latitud, Double longitud) {
        return new CrearActividadRequest(
                "Running en grupo", "Salidas grupales", tipo.getId(), nivel.getId(),
                new BigDecimal("4500"), "Parque Gral. San Martín", "gradient", 16, latitud, longitud);
    }

    private PerfilInstructor perfilAprobado() {
        PerfilInstructor perfil = new PerfilInstructor(instructor, "Running", 6, "desc");
        perfil.setEstadoVerificacion(EstadoVerificacion.APROBADO);
        return perfil;
    }

    @Test
    void crear_instructorAprobado_creaActividad() {
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfilAprobado()));
        when(tipoActividadRepository.findById(tipo.getId())).thenReturn(Optional.of(tipo));
        when(nivelIntensidadRepository.findById(nivel.getId())).thenReturn(Optional.of(nivel));
        when(usuarioRepository.findById(instructorId)).thenReturn(Optional.of(instructor));
        when(actividadRepository.saveAndFlush(any(Actividad.class))).thenAnswer(inv -> {
            Actividad a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
            return a;
        });

        CrearActividadResponse response = service.crear(requestValido(), instructorId);

        assertThat(response.nombre()).isEqualTo("Running en grupo");
        assertThat(response.tipoActividad().nombre()).isEqualTo("Aventura");
        assertThat(response.instructor().nombre()).isEqualTo("Mateo");
        assertThat(response.clases()).isEmpty();
        verify(auditService).registrar(eq(instructorId), eq(AuditAccion.ACTIVIDAD_CREADA), eq("Actividad"), any(UUID.class), isNull());
    }

    @Test
    void crear_instructorNoAprobado_lanzaSinPermiso() {
        PerfilInstructor perfil = new PerfilInstructor(instructor, "Running", 6, "desc");
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> service.crear(requestValido(), instructorId))
                .isInstanceOf(SinPermisoException.class);

        verify(actividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void crear_tipoActividadInexistente_lanzaNoEncontrado() {
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfilAprobado()));
        when(tipoActividadRepository.findById(tipo.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(requestValido(), instructorId))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void crear_conCoordenadas_lasPersisteYDevuelve() {
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfilAprobado()));
        when(tipoActividadRepository.findById(tipo.getId())).thenReturn(Optional.of(tipo));
        when(nivelIntensidadRepository.findById(nivel.getId())).thenReturn(Optional.of(nivel));
        when(usuarioRepository.findById(instructorId)).thenReturn(Optional.of(instructor));
        when(actividadRepository.saveAndFlush(any(Actividad.class))).thenAnswer(inv -> {
            Actividad a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
            return a;
        });

        CrearActividadResponse response = service.crear(requestConCoordenadas(-32.8908, -68.8272), instructorId);

        assertThat(response.latitud()).isEqualTo(-32.8908);
        assertThat(response.longitud()).isEqualTo(-68.8272);
    }

    @Test
    void crear_soloLatitudSinLongitud_lanzaValidacion() {
        when(perfilInstructorRepository.findByUsuarioId(instructorId)).thenReturn(Optional.of(perfilAprobado()));
        when(tipoActividadRepository.findById(tipo.getId())).thenReturn(Optional.of(tipo));
        when(nivelIntensidadRepository.findById(nivel.getId())).thenReturn(Optional.of(nivel));
        when(usuarioRepository.findById(instructorId)).thenReturn(Optional.of(instructor));

        assertThatThrownBy(() -> service.crear(requestConCoordenadas(-32.8908, null), instructorId))
                .isInstanceOf(ValidacionException.class);

        verify(actividadRepository, never()).saveAndFlush(any());
    }
}
