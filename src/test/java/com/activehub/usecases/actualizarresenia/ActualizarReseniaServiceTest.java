package com.activehub.usecases.actualizarresenia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
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
class ActualizarReseniaServiceTest {

    @Mock private ReseniaRepository reseniaRepository;
    @Mock private ActividadRepository actividadRepository;
    @Mock private AuditService auditService;

    @InjectMocks private ActualizarReseniaService service;

    private UUID reseniaId;
    private UUID alumnoId;
    private UUID actividadId;
    private Resenia resenia;

    @BeforeEach
    void setUp() {
        reseniaId = UUID.randomUUID();
        alumnoId = UUID.randomUUID();
        actividadId = UUID.randomUUID();

        Actividad actividad = new Actividad();
        ReflectionTestUtils.setField(actividad, "id", actividadId);
        Clase clase = new Clase();
        clase.setActividad(actividad);
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());

        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        resenia = new Resenia();
        resenia.setClase(clase);
        resenia.setAlumno(alumno);
        resenia.setPuntaje(3);
        resenia.setComentario("Estuvo bien");
        resenia.setEnModeracion(false);
        ReflectionTestUtils.setField(resenia, "id", reseniaId);
    }

    @Test
    void actualizar_reseniaPropia_cambiaPuntajeYRecalculaRating() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        ActualizarReseniaResponse response =
                service.actualizar(reseniaId, new ActualizarReseniaRequest(5, "  Excelente  "), alumnoId);

        assertThat(response.puntaje()).isEqualTo(5);
        assertThat(resenia.getComentario()).isEqualTo("Excelente");
        verify(actividadRepository).recalcularRating(actividadId);
        verify(auditService).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void actualizar_vuelveAModeracion() {
        // Cambió el contenido: el admin todavía no aprobó esta versión.
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        service.actualizar(reseniaId, new ActualizarReseniaRequest(4, "Otro comentario"), alumnoId);

        assertThat(resenia.isEnModeracion()).isTrue();
    }

    @Test
    void actualizar_reseniaAjena_lanzaSinPermiso() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        assertThatThrownBy(() ->
                service.actualizar(reseniaId, new ActualizarReseniaRequest(1, "No fui yo"), UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);

        verify(reseniaRepository, never()).save(any());
    }

    @Test
    void actualizar_reseniaInexistente_lanzaNoEncontrado() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.actualizar(reseniaId, new ActualizarReseniaRequest(5, "Hola"), alumnoId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
