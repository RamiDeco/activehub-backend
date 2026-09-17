package com.activehub.usecases.eliminaractividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.ActividadConInscriptosException;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.security.InstructorVerificadoGuard;
import com.activehub.shared.security.PenalizacionVigenteGuard;
import java.time.Instant;
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
class EliminarActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private InstructorVerificadoGuard instructorVerificadoGuard;
    @Mock
    private PenalizacionVigenteGuard penalizacionVigenteGuard;

    private EliminarActividadService service;
    private UUID actividadId;
    private UUID instructorId;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        service = new EliminarActividadService(
                actividadRepository, claseRepository, inscripcionRepository, auditService,
                penalizacionVigenteGuard, notificacionService, instructorVerificadoGuard);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        actividadId = UUID.randomUUID();
        actividad = new Actividad();
        actividad.setNombre("Running");
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", actividadId);
    }

    private Clase claseProgramada() {
        Clase clase = new Clase();
        clase.setActividad(actividad);
        clase.setEstado(EstadoClase.Programada);
        clase.setFechaHora(Instant.parse("2026-08-20T12:00:00Z"));
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());
        return clase;
    }

    @Test
    void eliminar_dueño_marcaBorrado() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(any(), any())).thenReturn(List.of());

        service.eliminar(actividadId, instructorId, false);

        assertThat(actividad.isDeleted()).isTrue();
        verify(actividadRepository).save(actividad);
        verify(notificacionService, never()).notificar(eq(instructorId), any(), any(), any(), any());
    }

    @Test
    void eliminar_admin_puedeBorrarActividadDeOtroInstructor() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(any(), any())).thenReturn(List.of());

        service.eliminar(actividadId, UUID.randomUUID(), true);

        assertThat(actividad.isDeleted()).isTrue();
        verify(actividadRepository).save(actividad);
        verify(notificacionService).notificar(eq(instructorId), eq(TipoNotificacion.ACTIVIDAD_ELIMINADA), any(), eq(actividadId), eq(Destino.ninguno()));
    }

    @Test
    void eliminar_conClaseVigenteConInscriptos_lanzaYNoBorraNada() {
        // E2I-HU06 criterio 7: el camino correcto es cancelar esas clases primero.
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        Clase clase = claseProgramada();
        when(claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(any(), any())).thenReturn(List.of(clase));

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setEstado(EstadoInscripcion.INSCRIPTO);
        when(inscripcionRepository.findByClaseIdAndEstadoNot(clase.getId(), EstadoInscripcion.CANCELADA))
                .thenReturn(List.of(inscripcion));

        assertThatThrownBy(() -> service.eliminar(actividadId, instructorId, false))
                .isInstanceOf(ActividadConInscriptosException.class)
                .hasMessageContaining("Primero cancelá las clases");

        assertThat(actividad.isDeleted()).isFalse();
        assertThat(clase.getEstado()).isEqualTo(EstadoClase.Programada);
        verify(actividadRepository, never()).save(any());
        verify(claseRepository, never()).save(any());
        verify(notificacionService, never()).notificar(any(), any(), any(), any(), any());
    }

    @Test
    void eliminar_conClasesVigentesVacias_lasCancelaYBorraLaActividad() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        Clase clase = claseProgramada();
        when(claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(any(), any())).thenReturn(List.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(clase.getId(), EstadoInscripcion.CANCELADA))
                .thenReturn(List.of());

        service.eliminar(actividadId, instructorId, false);

        assertThat(clase.getEstado()).isEqualTo(EstadoClase.Cancelada);
        assertThat(actividad.isDeleted()).isTrue();
        verify(claseRepository).save(clase);
    }

    @Test
    void eliminar_noTocaClasesYaTerminales() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(any(), any())).thenReturn(List.of());

        service.eliminar(actividadId, instructorId, false);

        verify(inscripcionRepository, never()).findByClaseIdAndEstadoNot(any(), any());
        verify(claseRepository, never()).save(any());
    }

    @Test
    void eliminar_noDueño_lanzaSinPermiso() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));

        assertThatThrownBy(() -> service.eliminar(actividadId, UUID.randomUUID(), false))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void eliminar_inexistente_lanzaNoEncontrado() {
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(actividadId, instructorId, false))
                .isInstanceOf(NoEncontradoException.class);
    }
}
