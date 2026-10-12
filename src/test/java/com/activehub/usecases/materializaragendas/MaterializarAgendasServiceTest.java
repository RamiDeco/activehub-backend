package com.activehub.usecases.materializaragendas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.AgendaClases;
import com.activehub.domain.actividad.AgendaClasesRepository;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.favorito.FavoritoRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.time.Zonas;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MaterializarAgendasServiceTest {

    /** Lunes 5 de octubre de 2026, 09:00 en Argentina. */
    private static final Instant AHORA =
            ZonedDateTime.of(LocalDate.of(2026, 10, 5), LocalTime.of(9, 0), Zonas.AR).toInstant();

    @Mock private AgendaClasesRepository agendaClasesRepository;
    @Mock private ClaseRepository claseRepository;
    @Mock private FavoritoRepository favoritoRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private AuditService auditService;

    private MaterializarAgendasService service;
    private UUID actividadId;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        service = new MaterializarAgendasService(
                agendaClasesRepository, claseRepository, favoritoRepository, notificacionService, auditService,
                Clock.fixed(AHORA, ZoneOffset.UTC));

        actividadId = UUID.randomUUID();
        actividad = new Actividad();
        actividad.setNombre("Yoga");
        ReflectionTestUtils.setField(actividad, "id", actividadId);
    }

    private AgendaClases agenda(DayOfWeek dia, LocalDate desde, LocalDate hasta) {
        AgendaClases a = new AgendaClases();
        a.setActividad(actividad);
        a.setDiaSemanaEnum(dia);
        a.setHoraInicio(LocalTime.of(18, 0));
        a.setHoraFin(LocalTime.of(19, 0));
        a.setCuposMax(15);
        a.setVigenciaDesde(desde);
        a.setVigenciaHasta(hasta);
        ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
        return a;
    }

    private void claseSeGuardaConId() {
        when(claseRepository.save(any(Clase.class))).thenAnswer(inv -> {
            Clase c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });
    }

    @Test
    void materializar_creaLaClaseDeLaSemanaConElHorarioDeLaAgenda() {
        AgendaClases a = agenda(DayOfWeek.WEDNESDAY, LocalDate.of(2026, 9, 1), null);
        when(agendaClasesRepository.findVigentesConDetalle(LocalDate.of(2026, 10, 5))).thenReturn(List.of(a));
        claseSeGuardaConId();

        assertThat(service.materializar()).isEqualTo(1);

        ArgumentCaptor<Clase> captor = ArgumentCaptor.forClass(Clase.class);
        verify(claseRepository).save(captor.capture());
        Clase clase = captor.getValue();

        Instant esperadoInicio =
                ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(18, 0), Zonas.AR).toInstant();
        assertThat(clase.getFechaHora()).isEqualTo(esperadoInicio);
        assertThat(clase.getHoraFin()).isEqualTo(esperadoInicio.plusSeconds(3600));
        assertThat(clase.getCuposMax()).isEqualTo(15);
        assertThat(clase.getEstado()).isEqualTo(EstadoClase.Programada);
        assertThat(clase.getAgendaClases()).isSameAs(a);
    }

    @Test
    void materializar_esIdempotente_noDuplicaLaClaseDeLaSemana() {
        // El job corre cada hora; sin esta guarda crearía una clase por corrida.
        AgendaClases a = agenda(DayOfWeek.WEDNESDAY, LocalDate.of(2026, 9, 1), null);
        when(agendaClasesRepository.findVigentesConDetalle(any())).thenReturn(List.of(a));
        when(claseRepository.existsByAgendaClasesIdAndFechaHora(any(), any())).thenReturn(true);

        assertThat(service.materializar()).isZero();
        verify(claseRepository, never()).save(any());
    }

    @Test
    void materializar_noPisaUnaClaseSueltaEnElMismoHorario() {
        AgendaClases a = agenda(DayOfWeek.WEDNESDAY, LocalDate.of(2026, 9, 1), null);
        when(agendaClasesRepository.findVigentesConDetalle(any())).thenReturn(List.of(a));
        when(claseRepository.existeSolapamiento(any(), any(), any(), any())).thenReturn(true);

        assertThat(service.materializar()).isZero();
        verify(claseRepository, never()).save(any());
    }

    @Test
    void materializar_agendaVencidaEstaSemana_noCreaNada() {
        // Vigencia cortada el lunes: el miércoles ya está fuera.
        AgendaClases a = agenda(DayOfWeek.WEDNESDAY, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 5));
        when(agendaClasesRepository.findVigentesConDetalle(any())).thenReturn(List.of(a));

        assertThat(service.materializar()).isZero();
        verify(claseRepository, never()).save(any());
    }

    @Test
    void materializar_ocurrenciaFueraDeLaVentanaDeUnaSemana_noSeAdelanta() {
        // La agenda arranca dentro de un mes: todavía no hay nada que instanciar.
        AgendaClases a = agenda(DayOfWeek.WEDNESDAY, LocalDate.of(2026, 11, 4), null);
        when(agendaClasesRepository.findVigentesConDetalle(any())).thenReturn(List.of(a));

        assertThat(service.materializar()).isZero();
        verify(claseRepository, never()).save(any());
    }

    @Test
    void materializar_sinAgendas_noHaceNada() {
        when(agendaClasesRepository.findVigentesConDetalle(any())).thenReturn(List.of());

        assertThat(service.materializar()).isZero();
        verify(favoritoRepository, never()).findByActividadId(any());
    }
}
