package com.activehub.usecases.ocultarresenia;

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
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OcultarReseniaServiceTest {

    @Mock private ReseniaRepository reseniaRepository;
    @Mock private ActividadRepository actividadRepository;
    @Mock private AuditService auditService;
    @Mock private NotificacionService notificacionService;

    @InjectMocks private OcultarReseniaService service;

    private UUID reseniaId;
    private UUID actividadId;
    private UUID alumnoId;
    private Resenia resenia;

    @BeforeEach
    void setUp() {
        actividadId = UUID.randomUUID();
        Actividad actividad = new Actividad();
        actividad.setNombre("Running");
        ReflectionTestUtils.setField(actividad, "id", actividadId);

        Clase clase = new Clase();
        clase.setActividad(actividad);
        clase.setFechaHora(Instant.parse("2026-08-01T12:00:00Z"));

        alumnoId = UUID.randomUUID();
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        reseniaId = UUID.randomUUID();
        resenia = new Resenia();
        resenia.setClase(clase);
        resenia.setAlumno(alumno);
        // Publicada: es el caso que este slice existe para cubrir.
        resenia.setEnModeracion(false);
        resenia.setOculta(false);
        ReflectionTestUtils.setField(resenia, "id", reseniaId);
    }

    private OcultarReseniaRequest req() {
        return new OcultarReseniaRequest("Contenido ofensivo hacia otro alumno");
    }

    @Test
    void ocultar_reseniaPublicada_laBajaYRecalculaElRating() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        OcultarReseniaResponse response = service.ocultar(reseniaId, req(), UUID.randomUUID());

        assertThat(response.oculta()).isTrue();
        assertThat(resenia.isOculta()).isTrue();
        verify(actividadRepository).recalcularRating(actividadId);
    }

    /**
     * El punto de ocultar en vez de borrar: si el contenido llega a ser algo en lo que deba
     * intervenir la justicia, la resenia y su autor tienen que seguir existiendo.
     */
    @Test
    void ocultar_noBorraLaReseniaNiPierdeAlAutor() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        service.ocultar(reseniaId, req(), UUID.randomUUID());

        assertThat(resenia.isDeleted()).isFalse();
        assertThat(resenia.getAlumno().getId()).isEqualTo(alumnoId);
    }

    @Test
    void ocultar_auditaConElMotivoYAvisaAlAutor() {
        UUID adminId = UUID.randomUUID();
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        service.ocultar(reseniaId, req(), adminId);

        verify(auditService).registrar(
                eq(adminId), eq(AuditAccion.RESENIA_OCULTADA), eq("Resenia"), eq(reseniaId),
                eq("Contenido ofensivo hacia otro alumno"));
        verify(notificacionService).notificar(
                eq(alumnoId), eq(TipoNotificacion.RESENIA_RECHAZADA), any(), eq(reseniaId), eq(Destino.resenia(reseniaId)));
    }

    @Test
    void ocultar_yaOculta_lanzaValidacion() {
        resenia.setOculta(true);
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        assertThatThrownBy(() -> service.ocultar(reseniaId, req(), UUID.randomUUID()))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("ya está oculta");

        verify(actividadRepository, never()).recalcularRating(any());
    }

    /**
     * Para una resenia que todavia no se publico el camino es rechazarla en la cola de
     * moderacion; ocultar algo que nadie vio dejaria dos mecanismos para el mismo estado.
     */
    @Test
    void ocultar_enModeracion_lanzaValidacionYSugiereRechazar() {
        resenia.setEnModeracion(true);
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        assertThatThrownBy(() -> service.ocultar(reseniaId, req(), UUID.randomUUID()))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("rechazala");
    }

    @Test
    void ocultar_inexistente_lanzaNoEncontrado() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ocultar(reseniaId, req(), UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
