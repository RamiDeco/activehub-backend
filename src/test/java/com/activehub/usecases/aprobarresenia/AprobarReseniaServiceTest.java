package com.activehub.usecases.aprobarresenia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
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
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AprobarReseniaServiceTest {

    @Mock
    private ReseniaRepository reseniaRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private NotificacionService notificacionService;

    private AprobarReseniaService service;
    private UUID reseniaId;
    private UUID actividadId;
    private UUID alumnoId;
    private Resenia resenia;

    @BeforeEach
    void setUp() {
        service = new AprobarReseniaService(reseniaRepository, actividadRepository, auditService, notificacionService);

        actividadId = UUID.randomUUID();
        Actividad actividad = new Actividad();
        actividad.setNombre("Running");
        ReflectionTestUtils.setField(actividad, "id", actividadId);

        Clase clase = new Clase();
        clase.setActividad(actividad);
        clase.setFechaHora(Instant.parse("2026-08-01T00:00:00Z"));

        alumnoId = UUID.randomUUID();
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        reseniaId = UUID.randomUUID();
        resenia = new Resenia();
        resenia.setClase(clase);
        resenia.setAlumno(alumno);
        resenia.setEnModeracion(true);
        ReflectionTestUtils.setField(resenia, "id", reseniaId);
    }

    @Test
    void aprobar_sacaDeModeracionYRecalculaRating() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        AprobarReseniaResponse response = service.aprobar(reseniaId, UUID.randomUUID());

        assertThat(response.enModeracion()).isFalse();
        assertThat(resenia.isEnModeracion()).isFalse();
        verify(actividadRepository).recalcularRating(actividadId);
        verify(notificacionService).notificar(eq(alumnoId), eq(TipoNotificacion.RESENIA_APROBADA), any(), eq(reseniaId), eq(Destino.resenia(reseniaId)));
    }

    /**
     * Regresion: aprobar una reseña devolvia 500.
     *
     * <p>{@code ActividadRepository.recalcularRating} es un {@code @Modifying} con
     * {@code clearAutomatically = true}: limpia el contexto de persistencia, la entidad queda
     * detached y toda relacion LAZY sin inicializar explota despues. El Service leia
     * {@code resenia.getAlumno()} DESPUES del recalculo, asi que en runtime saltaba
     * {@code LazyInitializationException} — y ningun test lo veia, porque con mocks la entidad
     * nunca se detacha.
     *
     * <p>Se simula el detach: apenas se llama a {@code recalcularRating}, los getters LAZY de la
     * reseña empiezan a fallar. Si alguien vuelve a mover una lectura despues del recalculo,
     * este test falla.
     */
    @Test
    void aprobar_noLeeRelacionesLazyDespuesDeRecalcularElRating() {
        Resenia espia = spy(resenia);
        boolean[] contextoLimpio = {false};
        doAnswer(inv -> {
            contextoLimpio[0] = true;
            return null;
        }).when(actividadRepository).recalcularRating(actividadId);
        doAnswer(inv -> {
            if (contextoLimpio[0]) {
                throw new org.hibernate.LazyInitializationException("contexto de persistencia limpiado");
            }
            return inv.callRealMethod();
        }).when(espia).getAlumno();
        doAnswer(inv -> {
            if (contextoLimpio[0]) {
                throw new org.hibernate.LazyInitializationException("contexto de persistencia limpiado");
            }
            return inv.callRealMethod();
        }).when(espia).getClase();
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(espia));

        service.aprobar(reseniaId, UUID.randomUUID());

        verify(notificacionService).notificar(eq(alumnoId), eq(TipoNotificacion.RESENIA_APROBADA), any(), eq(reseniaId), eq(Destino.resenia(reseniaId)));
    }

    @Test
    void aprobar_inexistente_lanzaNoEncontrado() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aprobar(reseniaId, UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
