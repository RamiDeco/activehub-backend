package com.activehub.usecases.responderresenia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.time.Clock;
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
class ResponderReseniaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-07-01T10:00:00Z");

    @Mock private ReseniaRepository reseniaRepository;
    @Mock private InstructorVerificadoGuard instructorVerificadoGuard;
    @Mock private NotificacionService notificacionService;
    @Mock private AuditService auditService;

    private ResponderReseniaService service;
    private UUID reseniaId;
    private UUID instructorId;
    private UUID alumnoId;
    private Resenia resenia;

    @BeforeEach
    void setUp() {
        service = new ResponderReseniaService(
                reseniaRepository, instructorVerificadoGuard, notificacionService, auditService,
                Clock.fixed(AHORA, ZoneOffset.UTC));

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setNombre("Yoga");
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        Clase clase = new Clase();
        clase.setActividad(actividad);
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());

        alumnoId = UUID.randomUUID();
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        reseniaId = UUID.randomUUID();
        resenia = new Resenia();
        resenia.setClase(clase);
        resenia.setAlumno(alumno);
        resenia.setPuntaje(4);
        resenia.setComentario("Buena clase");
        resenia.setEnModeracion(false);
        ReflectionTestUtils.setField(resenia, "id", reseniaId);
    }

    @Test
    void responder_guardaLaRespuestaYNotificaAlAlumno() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        ResponderReseniaResponse response =
                service.responder(reseniaId, new ResponderReseniaRequest("  ¡Gracias!  "), instructorId);

        assertThat(response.respuestaInstructor()).isEqualTo("¡Gracias!");
        assertThat(response.respuestaInstructorAt()).isEqualTo(AHORA);
        assertThat(resenia.getRespuestaInstructor()).isEqualTo("¡Gracias!");
        verify(reseniaRepository).save(resenia);
        verify(notificacionService).notificar(
                eq(alumnoId), eq(TipoNotificacion.RESENIA_RESPONDIDA), any(), eq(reseniaId), eq(Destino.resenia(reseniaId)));
    }

    @Test
    void responder_reseniaDeOtroInstructor_lanzaSinPermiso() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        assertThatThrownBy(() ->
                service.responder(reseniaId, new ResponderReseniaRequest("Hola"), UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);

        verify(reseniaRepository, never()).save(any());
    }

    @Test
    void responder_reseniaEnModeracion_lanzaValidacion() {
        // Publicaría la respuesta antes que el comentario que contesta.
        resenia.setEnModeracion(true);
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        assertThatThrownBy(() ->
                service.responder(reseniaId, new ResponderReseniaRequest("Hola"), instructorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("moderación");
    }

    @Test
    void responder_reseniaOculta_lanzaValidacion() {
        resenia.setOculta(true);
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        assertThatThrownBy(() ->
                service.responder(reseniaId, new ResponderReseniaRequest("Hola"), instructorId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void responder_reseniaInexistente_lanzaNoEncontrado() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.responder(reseniaId, new ResponderReseniaRequest("Hola"), instructorId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
