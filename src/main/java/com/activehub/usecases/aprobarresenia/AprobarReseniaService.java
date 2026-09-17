package com.activehub.usecases.aprobarresenia;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AprobarReseniaService {

    private final ReseniaRepository reseniaRepository;
    private final ActividadRepository actividadRepository;
    private final AuditService auditService;
    private final NotificacionService notificacionService;

    public AprobarReseniaService(
            ReseniaRepository reseniaRepository,
            ActividadRepository actividadRepository,
            AuditService auditService,
            NotificacionService notificacionService
    ) {
        this.reseniaRepository = reseniaRepository;
        this.actividadRepository = actividadRepository;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
    }

    /**
     * <b>Todo lo que haga falta de la reseña se lee ANTES de {@code recalcularRating}.</b> Ese
     * {@code @Modifying} va con {@code clearAutomatically = true}, asi que limpia el contexto de
     * persistencia: despues de llamarlo la entidad queda detached y cualquier relacion LAZY que
     * todavia no se haya tocado explota con {@code LazyInitializationException}.
     *
     * <p>Era exactamente eso — {@code resenia.getAlumno()} leido despues del recalculo — lo que
     * hacia que aprobar una reseña devolviera 500, mientras rechazarla, que ya leia todo antes,
     * funcionaba bien. Si agregas un dato de la reseña a la notificacion, leelo arriba.
     */
    @Transactional
    public AprobarReseniaResponse aprobar(UUID id, UUID actorId) {
        Resenia resenia = reseniaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Reseña no encontrada."));

        UUID actividadId = resenia.getClase().getActividad().getId();
        UUID alumnoId = resenia.getAlumno().getId();
        String contexto = "la clase de \"" + resenia.getClase().getActividad().getNombre()
                + "\" del " + NotificacionMensajes.formatFechaHora(resenia.getClase().getFechaHora());

        resenia.setEnModeracion(false);
        reseniaRepository.save(resenia);
        actividadRepository.recalcularRating(actividadId);

        notificacionService.notificar(
                alumnoId,
                TipoNotificacion.RESENIA_APROBADA,
                "Tu reseña sobre " + contexto + " fue publicada.",
                id, Destino.resenia(id));

        auditService.registrar(actorId, AuditAccion.RESENIA_APROBADA, "Resenia", id, null);

        return new AprobarReseniaResponse(id, false);
    }
}
