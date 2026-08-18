package com.activehub.usecases.creardenuncia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
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
class CrearDenunciaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-08-20T00:00:00Z");

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private DenunciaRepository denunciaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditService auditService;

    private CrearDenunciaService service;
    private UUID claseId;
    private UUID alumnoId;
    private Clase clase;
    private CrearDenunciaRequest request;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        service = new CrearDenunciaService(claseRepository, inscripcionRepository, denunciaRepository, usuarioRepository, auditService, clock);

        claseId = UUID.randomUUID();
        alumnoId = UUID.randomUUID();
        clase = new Clase();
        clase.setFechaHora(AHORA.minus(Duration.ofHours(2)));
        ReflectionTestUtils.setField(clase, "id", claseId);

        request = new CrearDenunciaRequest("El instructor no se presentó.");
    }

    private void stubElegible() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.existsByClaseIdAndAlumnoIdAndEstado(claseId, alumnoId, EstadoInscripcion.INSCRIPTO))
                .thenReturn(true);
        when(denunciaRepository.existsByClaseIdAndAlumnoId(claseId, alumnoId)).thenReturn(false);
    }

    @Test
    void crear_alumnoElegible_creaPendiente() {
        stubElegible();
        when(usuarioRepository.getReferenceById(alumnoId)).thenReturn(new Usuario());
        when(denunciaRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Denuncia.class))).thenAnswer(inv -> {
            Denuncia d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(d, "createdAt", AHORA);
            return d;
        });

        CrearDenunciaResponse response = service.crear(claseId, request, alumnoId);

        assertThat(response.estado()).isEqualTo("Pendiente");
        assertThat(response.motivo()).isEqualTo("El instructor no se presentó.");
    }

    @Test
    void crear_sinInscripcionInscripto_lanzaValidacion() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.existsByClaseIdAndAlumnoIdAndEstado(claseId, alumnoId, EstadoInscripcion.INSCRIPTO))
                .thenReturn(false);

        assertThatThrownBy(() -> service.crear(claseId, request, alumnoId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void crear_antesDeUnaHora_lanzaValidacion() {
        clase.setFechaHora(AHORA.minus(Duration.ofMinutes(30)));
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.existsByClaseIdAndAlumnoIdAndEstado(claseId, alumnoId, EstadoInscripcion.INSCRIPTO))
                .thenReturn(true);

        assertThatThrownBy(() -> service.crear(claseId, request, alumnoId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void crear_yaDenunciada_lanzaValidacion() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.existsByClaseIdAndAlumnoIdAndEstado(claseId, alumnoId, EstadoInscripcion.INSCRIPTO))
                .thenReturn(true);
        when(denunciaRepository.existsByClaseIdAndAlumnoId(claseId, alumnoId)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(claseId, request, alumnoId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void crear_claseInexistente_lanzaNoEncontrado() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(claseId, request, alumnoId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
