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
import com.activehub.shared.security.PenalizacionVigenteGuard;
import java.time.Clock;
import java.time.ZoneOffset;
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
    @org.mockito.Mock private PenalizacionVigenteGuard penalizacionVigenteGuard;


    private ActualizarClaseService service;
    private UUID claseId;
    private UUID instructorId;
    private Clase clase;

    @BeforeEach
    void setUp() {
        service = new ActualizarClaseService(
                claseRepository, auditService, penalizacionVigenteGuard, instructorVerificadoGuard,
                Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setInstructor(instructor);
        // La hora de fin sale de aca, no del request (ver ActualizarClaseRequest).
        actividad.setDuracionMin(60);
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

        ActualizarClaseRequest request = new ActualizarClaseRequest(nuevoInicio, 12);
        ActualizarClaseResponse response = service.actualizar(claseId, request, instructorId);

        assertThat(response.cuposMax()).isEqualTo(12);
        assertThat(response.horaFin()).isEqualTo(nuevoFin);
    }

    @Test
    void actualizar_cuposMaxMenorAOcupados_lanzaValidacion() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        ActualizarClaseRequest request = new ActualizarClaseRequest(nuevoInicio, 3);
        assertThatThrownBy(() -> service.actualizar(claseId, request, instructorId))
                .isInstanceOf(ValidacionException.class);
    }

    /**
     * Reemplaza al viejo "hora fin anterior al inicio": ese caso ya no puede existir porque la
     * hora de fin no viene del cliente, la calcula el Service con {@code actividad.duracionMin}
     * (que tiene un CHECK &gt; 0 en la base). Lo que hay que fijar es que la clase editada dure
     * lo que promete su actividad.
     */
    @Test
    void actualizar_derivaLaHoraDeFinDeLaDuracionDeLaActividad() {
        clase.getActividad().setDuracionMin(45);
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        service.actualizar(claseId, new ActualizarClaseRequest(nuevoInicio, 12), instructorId);

        assertThat(clase.getHoraFin()).isEqualTo(nuevoInicio.plusSeconds(45 * 60));
    }

    @Test
    void actualizar_solapaConOtraClase_lanzaValidacion() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        // Se excluye la propia clase del chequeo: si no, editar el cupo sin tocar el horario
        // fallaría contra sí misma.
        when(claseRepository.existeSolapamiento(any(), eq(nuevoInicio), eq(nuevoFin), eq(claseId))).thenReturn(true);

        ActualizarClaseRequest request = new ActualizarClaseRequest(nuevoInicio, 12);
        assertThatThrownBy(() -> service.actualizar(claseId, request, instructorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("Ya existe una clase en ese horario");
    }

    @Test
    void actualizar_noDueño_lanzaSinPermiso() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        ActualizarClaseRequest request = new ActualizarClaseRequest(nuevoInicio, 12);
        assertThatThrownBy(() -> service.actualizar(claseId, request, UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);
    }
    /**
     * Clase CONGELADA: ya entro a la ventana de inscripcion (faltan <= 4 dias) y tiene al menos
     * un inscripto, o sea que hay gente que pago por estos datos. El reloj del test esta fijo en
     * 2026-01-01, asi que una clase del 03 cae dentro de la ventana.
     */
    @Test
    void actualizar_claseCongelada_lanzaValidacionYNoGuarda() {
        clase.setFechaHora(java.time.Instant.parse("2026-01-03T12:00:00Z"));
        clase.setCuposOcupados(3);
        when(claseRepository.findById(claseId)).thenReturn(java.util.Optional.of(clase));

        assertThatThrownBy(() -> service.actualizar(
                claseId,
                new ActualizarClaseRequest(java.time.Instant.parse("2026-01-04T12:00:00Z"), 10),
                instructorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("cancelala");

        org.mockito.Mockito.verify(claseRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void actualizar_enVentanaDeInscripcionPeroSinInscriptos_siDejaEditar() {
        // El congelamiento lo dispara la combinacion de las dos cosas: sin nadie anotado, una
        // clase proxima se sigue pudiendo corregir.
        clase.setFechaHora(java.time.Instant.parse("2026-01-03T12:00:00Z"));
        clase.setCuposOcupados(0);
        when(claseRepository.findById(claseId)).thenReturn(java.util.Optional.of(clase));

        service.actualizar(
                claseId,
                new ActualizarClaseRequest(java.time.Instant.parse("2026-01-04T12:00:00Z"), 10),
                instructorId);

        org.mockito.Mockito.verify(claseRepository).save(clase);
    }
}
