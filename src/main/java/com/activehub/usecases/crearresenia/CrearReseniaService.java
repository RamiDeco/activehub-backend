package com.activehub.usecases.crearresenia;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El alumno califica una clase finalizada a la que asistio. <b>La resenia se publica en el
 * acto</b>: se ve en el detalle de la actividad y suma al rating apenas se guarda.
 *
 * <p>Antes nacia en moderacion ({@code en_moderacion = true}) y no se veia hasta que un
 * administrador la aprobaba. Se retiro (V33): el administrador no revisa cada resenia, atiende
 * las <b>denuncias</b> que hace el instructor ({@code denunciarresenia} ->
 * {@code resolverdenuncia} con {@code OCULTAR_RESENIA}) y puede ocultar una publicada por su
 * cuenta ({@code ocultarresenia}). Una resenia publicada tampoco se edita ni se elimina: los
 * slices {@code actualizarresenia} y {@code eliminarresenia} se retiraron con el mismo cambio.
 */
@Service
public class CrearReseniaService {

    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final ReseniaRepository reseniaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ActividadRepository actividadRepository;
    private final AuditService auditService;
    private final NotificacionService notificacionService;

    public CrearReseniaService(
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            ReseniaRepository reseniaRepository,
            UsuarioRepository usuarioRepository,
            ActividadRepository actividadRepository,
            AuditService auditService,
            NotificacionService notificacionService
    ) {
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.reseniaRepository = reseniaRepository;
        this.usuarioRepository = usuarioRepository;
        this.actividadRepository = actividadRepository;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public CrearReseniaResponse crear(UUID claseId, CrearReseniaRequest request, UUID alumnoId) {
        Clase clase = claseRepository.findById(claseId)
                .orElseThrow(() -> new NoEncontradoException("Clase no encontrada."));

        if (!inscripcionRepository.existsByClaseIdAndAlumnoIdAndEstado(claseId, alumnoId, EstadoInscripcion.INSCRIPTO)) {
            throw new ValidacionException("Solo podés reseñar clases en las que estuviste inscripto.");
        }
        if (clase.getEstado() != EstadoClase.Finalizada) {
            throw new ValidacionException("Todavía no terminó la clase.");
        }
        if (reseniaRepository.existsByClaseIdAndAlumnoId(claseId, alumnoId)) {
            throw new ValidacionException("Ya dejaste una reseña para esta clase.");
        }

        Resenia resenia = new Resenia();
        resenia.setClase(clase);
        resenia.setAlumno(usuarioRepository.getReferenceById(alumnoId));
        resenia.setPuntaje(request.puntaje());
        resenia.setComentario(normalizar(request.comentario()));
        resenia = reseniaRepository.saveAndFlush(resenia);

        // Todo lo que haga falta de la clase y la actividad se lee ANTES de recalcularRating:
        // es un @Modifying con clearAutomatically, deja las entidades detached y una relacion
        // LAZY sin tocar explota despues con LazyInitializationException (ya paso al aprobar).
        UUID actividadId = clase.getActividad().getId();
        UUID instructorId = clase.getActividad().getInstructor().getId();
        String mensaje = "Recibiste una nueva reseña en la clase de \"" + clase.getActividad().getNombre()
                + "\" del " + NotificacionMensajes.formatFechaHora(clase.getFechaHora()) + ".";
        CrearReseniaResponse response = new CrearReseniaResponse(
                resenia.getId(), claseId, alumnoId, resenia.getPuntaje(), resenia.getComentario(),
                resenia.getCreatedAt());

        notificacionService.notificar(
                instructorId, TipoNotificacion.NUEVA_RESENIA, mensaje,
                resenia.getId(), Destino.resenia(resenia.getId()));
        auditService.registrar(alumnoId, AuditAccion.RESENIA_CREADA, "Resenia", resenia.getId(), null);

        // Publicada en el acto: suma al promedio de la actividad desde ahora.
        actividadRepository.recalcularRating(actividadId);

        return response;
    }

    /** Un comentario en blanco es "sin comentario", no una cadena vacía guardada. */
    private String normalizar(String comentario) {
        return comentario == null || comentario.isBlank() ? null : comentario.trim();
    }
}
