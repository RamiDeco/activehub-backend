package com.activehub.usecases.crearclase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.AgendaClases;
import com.activehub.domain.actividad.AgendaClasesRepository;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.favorito.ActividadFavorita;
import com.activehub.domain.favorito.FavoritoRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.security.InstructorVerificadoGuard;
import com.activehub.shared.time.Zonas;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    private AgendaClasesRepository agendaClasesRepository;
    @Mock
    private FavoritoRepository favoritoRepository;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private InstructorVerificadoGuard instructorVerificadoGuard;
    @Mock
    private AuditService auditService;

    private CrearClaseService service;
    private UUID actividadId;
    private UUID instructorId;
    private Actividad actividad;
    private Instant inicio;
    private Instant fin;

    @BeforeEach
    void setUp() {
        service = new CrearClaseService(
                actividadRepository, claseRepository, agendaClasesRepository, favoritoRepository,
                notificacionService, instructorVerificadoGuard, auditService);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        actividadId = UUID.randomUUID();
        actividad = new Actividad();
        actividad.setInstructor(instructor);
        actividad.setNombre("Yoga");
        ReflectionTestUtils.setField(actividad, "id", actividadId);

        // Un martes al mediodía, para que el día de la semana de la agenda sea predecible.
        ZonedDateTime local = ZonedDateTime.of(LocalDate.of(2026, 10, 6), LocalTime.of(12, 0), Zonas.AR);
        inicio = local.toInstant();
        fin = local.plusHours(1).toInstant();
    }

    private void claseSeGuardaConId() {
        when(claseRepository.save(any(Clase.class))).thenAnswer(inv -> {
            Clase c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });
    }

    @Test
    void crear_guardaHorarioCompletoYCuposDelRequest() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        claseSeGuardaConId();

        CrearClaseResponse response = service.crear(
                actividadId, new CrearClaseRequest(inicio, fin, 12, false, null), instructorId);

        assertThat(response.cuposMax()).isEqualTo(12);
        assertThat(response.horaFin()).isEqualTo(fin);
        assertThat(response.cuposOcupados()).isEqualTo(0);
        assertThat(response.estado()).isEqualTo("Programada");
        assertThat(response.agendaClasesId()).isNull();
        verify(agendaClasesRepository, never()).save(any());
    }

    @Test
    void crear_horaFinAnteriorAlInicio_lanzaValidacion() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        assertThatThrownBy(() -> service.crear(
                actividadId, new CrearClaseRequest(inicio, inicio.minusSeconds(1), 10, false, null), instructorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("posterior a la hora de inicio");

        verify(claseRepository, never()).save(any());
    }

    @Test
    void crear_horaFinIgualAlInicio_lanzaValidacion() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        assertThatThrownBy(() -> service.crear(
                actividadId, new CrearClaseRequest(inicio, inicio, 10, false, null), instructorId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void crear_solapaConOtraClase_lanzaValidacion() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(claseRepository.existeSolapamiento(eq(actividadId), eq(inicio), eq(fin), isNull())).thenReturn(true);

        assertThatThrownBy(() -> service.crear(
                actividadId, new CrearClaseRequest(inicio, fin, 10, false, null), instructorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("Ya existe una clase en ese horario");

        verify(claseRepository, never()).save(any());
    }

    @Test
    void crear_conRepeticion_guardaLaAgendaYLaEnlazaALaClase() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        claseSeGuardaConId();
        UUID agendaId = UUID.randomUUID();
        when(agendaClasesRepository.save(any(AgendaClases.class))).thenAnswer(inv -> {
            AgendaClases a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", agendaId);
            return a;
        });

        CrearClaseResponse response = service.crear(
                actividadId,
                new CrearClaseRequest(inicio, fin, 12, true, LocalDate.of(2026, 12, 31)),
                instructorId);

        assertThat(response.agendaClasesId()).isEqualTo(agendaId);

        var capturada = org.mockito.ArgumentCaptor.forClass(AgendaClases.class);
        verify(agendaClasesRepository).save(capturada.capture());
        AgendaClases agenda = capturada.getValue();
        assertThat(agenda.getDiaSemanaEnum()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(agenda.getHoraInicio()).isEqualTo(LocalTime.of(12, 0));
        assertThat(agenda.getHoraFin()).isEqualTo(LocalTime.of(13, 0));
        assertThat(agenda.getCuposMax()).isEqualTo(12);
        assertThat(agenda.getVigenciaDesde()).isEqualTo(LocalDate.of(2026, 10, 6));
        assertThat(agenda.getVigenciaHasta()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void crear_repeticionQueTerminaAntesDeLaPrimeraClase_lanzaValidacion() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        assertThatThrownBy(() -> service.crear(
                actividadId,
                new CrearClaseRequest(inicio, fin, 12, true, LocalDate.of(2026, 1, 1)),
                instructorId))
                .isInstanceOf(ValidacionException.class);

        verify(claseRepository, never()).save(any());
    }

    @Test
    void crear_repeticionDeClaseQueCruzaLaMedianoche_lanzaValidacion() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        ZonedDateTime nocturna = ZonedDateTime.of(LocalDate.of(2026, 10, 6), LocalTime.of(23, 0), Zonas.AR);
        assertThatThrownBy(() -> service.crear(
                actividadId,
                new CrearClaseRequest(nocturna.toInstant(), nocturna.plusHours(2).toInstant(), 10, true, null),
                instructorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("día siguiente");
    }

    @Test
    void crear_noDueño_lanzaSinPermiso() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        CrearClaseRequest request = new CrearClaseRequest(inicio, fin, 10, false, null);
        assertThatThrownBy(() -> service.crear(actividadId, request, UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void crear_instructorNoAprobado_lanzaSinPermiso() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        doThrow(new SinPermisoException("no verificado"))
                .when(instructorVerificadoGuard).exigirVerificado(eq(instructorId), any());

        CrearClaseRequest request = new CrearClaseRequest(inicio, fin, 10, false, null);
        assertThatThrownBy(() -> service.crear(actividadId, request, instructorId))
                .isInstanceOf(SinPermisoException.class);

        verify(claseRepository, never()).save(any());
    }

    @Test
    void crear_notificaAQuienesTienenLaActividadDeFavorita() {
        UUID alumnoId = UUID.randomUUID();
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);
        ActividadFavorita favorito = new ActividadFavorita(alumno, actividad);

        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(favoritoRepository.findByActividadId(actividadId)).thenReturn(List.of(favorito));
        claseSeGuardaConId();

        CrearClaseRequest request = new CrearClaseRequest(
                Instant.now().plus(3, ChronoUnit.DAYS), Instant.now().plus(3, ChronoUnit.DAYS).plusSeconds(3600),
                10, false, null);
        CrearClaseResponse response = service.crear(actividadId, request, instructorId);

        verify(notificacionService).notificar(
                eq(alumnoId), eq(TipoNotificacion.NUEVO_HORARIO_FAVORITO), any(), eq(response.id()));
    }
}
