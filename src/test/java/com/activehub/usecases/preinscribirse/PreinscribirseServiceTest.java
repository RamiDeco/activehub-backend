package com.activehub.usecases.preinscribirse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.security.InstructorVerificadoGuard;
import com.activehub.shared.error.InscripcionYaExisteException;
import com.activehub.shared.error.ValidacionException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PreinscribirseServiceTest {

    private static final Instant AHORA = Instant.parse("2026-08-01T00:00:00Z");

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditService auditService;

    @org.mockito.Mock private InstructorVerificadoGuard instructorVerificadoGuard;


    private PreinscribirseService service;
    private UUID claseId;
    private UUID alumnoId;
    private Clase clase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        service = new PreinscribirseService(claseRepository, inscripcionRepository, usuarioRepository, auditService, instructorVerificadoGuard, clock);

        claseId = UUID.randomUUID();
        alumnoId = UUID.randomUUID();
        // La clase necesita su Actividad e instructor: el service consulta el estado de
        // verificación del instructor antes de dejar preinscribir (RN-16).
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", UUID.randomUUID());
        Actividad actividad = new Actividad();
        actividad.setNombre("Yoga");
        actividad.setInstructor(instructor);

        clase = new Clase();
        clase.setEstado(EstadoClase.Programada);
        clase.setActividad(actividad);
        ReflectionTestUtils.setField(clase, "id", claseId);
    }

    @Test
    void preinscribirse_masDe4Dias_creaPreinscripcion() {
        clase.setFechaHora(AHORA.plus(Duration.ofDays(10)));
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.empty());
        when(usuarioRepository.getReferenceById(alumnoId)).thenReturn(new Usuario());
        when(inscripcionRepository.saveAndFlush(any(Inscripcion.class))).thenAnswer(inv -> {
            Inscripcion i = inv.getArgument(0);
            ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(i, "createdAt", AHORA);
            return i;
        });

        PreinscribirseResponse response = service.preinscribirse(claseId, alumnoId);

        assertThat(response.estado()).isEqualTo("PreInscripción");
        assertThat(response.pagoId()).isNull();
        verify(claseRepository, never()).ocuparCupo(any());
    }

    @Test
    void preinscribirse_4DiasOMenos_lanzaValidacion() {
        clase.setFechaHora(AHORA.plus(Duration.ofDays(2)));
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.preinscribirse(claseId, alumnoId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void preinscribirse_claseCancelada_lanzaValidacion() {
        clase.setEstado(EstadoClase.Cancelada);
        clase.setFechaHora(AHORA.plus(Duration.ofDays(10)));
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.preinscribirse(claseId, alumnoId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void preinscribirse_yaTieneInscripcionActiva_lanzaYaExiste() {
        clase.setFechaHora(AHORA.plus(Duration.ofDays(10)));
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.of(new Inscripcion()));

        assertThatThrownBy(() -> service.preinscribirse(claseId, alumnoId))
                .isInstanceOf(InscripcionYaExisteException.class);
    }
}
