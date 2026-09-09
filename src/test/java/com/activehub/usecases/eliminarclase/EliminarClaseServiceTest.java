package com.activehub.usecases.eliminarclase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.security.InstructorVerificadoGuard;
import com.activehub.shared.error.ClaseConInscriptosException;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EliminarClaseServiceTest {

    @Mock private ClaseRepository claseRepository;
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private AuditService auditService;

    @org.mockito.Mock private InstructorVerificadoGuard instructorVerificadoGuard;


    private EliminarClaseService service;
    private UUID claseId;
    private UUID instructorId;
    private Clase clase;

    @BeforeEach
    void setUp() {
        service = new EliminarClaseService(claseRepository, inscripcionRepository, auditService, instructorVerificadoGuard);

        claseId = UUID.randomUUID();
        instructorId = UUID.randomUUID();

        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setNombre("Yoga");
        actividad.setInstructor(instructor);

        clase = new Clase();
        clase.setActividad(actividad);
        ReflectionTestUtils.setField(clase, "id", claseId);
    }

    @Test
    void eliminar_sinInscriptos_haceBajaLogicaYAudita() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.existsByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA))
                .thenReturn(false);

        service.eliminar(claseId, instructorId);

        assertThat(clase.isDeleted()).isTrue();
        verify(claseRepository).save(clase);
        verify(auditService).registrar(
                ArgumentMatchers.eq(instructorId), ArgumentMatchers.eq(AuditAccion.CLASE_ELIMINADA),
                ArgumentMatchers.eq("Clase"), ArgumentMatchers.eq(claseId), ArgumentMatchers.isNull());
    }

    @Test
    void eliminar_conInscriptos_lanzaClaseConInscriptosYNoBorraNada() {
        // Si se borrara, la clase quedaria invisible por @SQLRestriction y el alumno
        // perderia del historial una clase que pago, sin pasar nunca por Cancelada.
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.existsByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA))
                .thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(claseId, instructorId))
                .isInstanceOf(ClaseConInscriptosException.class);

        assertThat(clase.isDeleted()).isFalse();
        verify(claseRepository, never()).save(any());
        verify(auditService, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void eliminar_claseAjena_lanzaSinPermiso() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.eliminar(claseId, UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);

        verify(claseRepository, never()).save(any());
    }

    @Test
    void eliminar_claseInexistente_lanzaNoEncontrado() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(claseId, instructorId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
