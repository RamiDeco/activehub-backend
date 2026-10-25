package com.activehub.usecases.crearresenia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CrearReseniaServiceTest {

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private ReseniaRepository reseniaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private NotificacionService notificacionService;

    private CrearReseniaService service;
    private UUID claseId;
    private UUID alumnoId;
    private UUID instructorId;
    private UUID actividadId;
    private Clase clase;
    private CrearReseniaRequest request;

    @BeforeEach
    void setUp() {
        service = new CrearReseniaService(
                claseRepository, inscripcionRepository, reseniaRepository, usuarioRepository, actividadRepository,
                auditService, notificacionService);

        claseId = UUID.randomUUID();
        alumnoId = UUID.randomUUID();
        instructorId = UUID.randomUUID();

        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);
        Actividad actividad = new Actividad();
        actividad.setNombre("Running");
        actividad.setInstructor(instructor);
        actividadId = UUID.randomUUID();
        ReflectionTestUtils.setField(actividad, "id", actividadId);

        clase = new Clase();
        clase.setEstado(EstadoClase.Finalizada);
        clase.setActividad(actividad);
        clase.setFechaHora(Instant.parse("2026-08-01T00:00:00Z"));
        ReflectionTestUtils.setField(clase, "id", claseId);

        request = new CrearReseniaRequest(5, "Excelente clase.");
    }

    private void stubElegible() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.existsByClaseIdAndAlumnoIdAndEstado(claseId, alumnoId, EstadoInscripcion.INSCRIPTO))
                .thenReturn(true);
        when(reseniaRepository.existsByClaseIdAndAlumnoId(claseId, alumnoId)).thenReturn(false);
    }

    private void stubGuardado() {
        when(usuarioRepository.getReferenceById(alumnoId)).thenReturn(new Usuario());
        when(reseniaRepository.saveAndFlush(any(Resenia.class))).thenAnswer(inv -> {
            Resenia r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(r, "createdAt", Instant.parse("2026-08-01T00:00:00Z"));
            return r;
        });
    }

    /**
     * La reseña se publica en el acto: no hay moderación previa (V33), así que tiene que sumar
     * al rating de la actividad apenas se guarda. Antes el recálculo lo hacía aprobarresenia.
     */
    @Test
    void crear_alumnoElegible_publicaYRecalculaElRating() {
        stubElegible();
        stubGuardado();

        CrearReseniaResponse response = service.crear(claseId, request, alumnoId);

        assertThat(response.puntaje()).isEqualTo(5);
        assertThat(response.comentario()).isEqualTo("Excelente clase.");
        InOrder orden = inOrder(reseniaRepository, actividadRepository);
        orden.verify(reseniaRepository).saveAndFlush(any(Resenia.class));
        orden.verify(actividadRepository).recalcularRating(actividadId);
        verify(notificacionService).notificar(
                eq(instructorId), eq(TipoNotificacion.NUEVA_RESENIA), any(), eq(response.id()),
                eq(Destino.resenia(response.id())));
    }

    /**
     * Regresión que heredó este slice de aprobarresenia (retirado): {@code recalcularRating}
     * es un {@code @Modifying} con {@code clearAutomatically = true}, deja la clase detached y
     * cualquier relación LAZY leída después explota con {@code LazyInitializationException}.
     * Con mocks la entidad nunca se detacha, así que se simula: apenas se recalcula, leer la
     * actividad de la clase empieza a fallar. Si alguien mueve una lectura después del
     * recálculo, este test falla.
     */
    @Test
    void crear_noLeeRelacionesLazyDespuesDeRecalcularElRating() {
        Clase espia = spy(clase);
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
        }).when(espia).getActividad();
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(espia));
        when(inscripcionRepository.existsByClaseIdAndAlumnoIdAndEstado(claseId, alumnoId, EstadoInscripcion.INSCRIPTO))
                .thenReturn(true);
        when(reseniaRepository.existsByClaseIdAndAlumnoId(claseId, alumnoId)).thenReturn(false);
        stubGuardado();

        CrearReseniaResponse response = service.crear(claseId, request, alumnoId);

        assertThat(response.claseId()).isEqualTo(claseId);
        verify(notificacionService).notificar(eq(instructorId), eq(TipoNotificacion.NUEVA_RESENIA), any(), any(), any());
    }

    @Test
    void crear_comentarioEnBlanco_seGuardaSinComentario() {
        stubElegible();
        stubGuardado();

        CrearReseniaResponse response = service.crear(claseId, new CrearReseniaRequest(4, "   "), alumnoId);

        assertThat(response.comentario()).isNull();
        verify(actividadRepository).recalcularRating(actividadId);
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
    void crear_claseNoFinalizada_lanzaValidacion() {
        clase.setEstado(EstadoClase.Programada);
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.existsByClaseIdAndAlumnoIdAndEstado(claseId, alumnoId, EstadoInscripcion.INSCRIPTO))
                .thenReturn(true);

        assertThatThrownBy(() -> service.crear(claseId, request, alumnoId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void crear_yaExisteResenia_lanzaValidacion() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.existsByClaseIdAndAlumnoIdAndEstado(claseId, alumnoId, EstadoInscripcion.INSCRIPTO))
                .thenReturn(true);
        when(reseniaRepository.existsByClaseIdAndAlumnoId(claseId, alumnoId)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(claseId, request, alumnoId))
                .isInstanceOf(ValidacionException.class);
        verify(reseniaRepository, never()).saveAndFlush(any());
        verify(actividadRepository, never()).recalcularRating(any());
    }

    @Test
    void crear_claseInexistente_lanzaNoEncontrado() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(claseId, request, alumnoId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
