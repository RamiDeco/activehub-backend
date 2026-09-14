package com.activehub.usecases.rechazarresenia;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RechazarReseniaService {

    private final ReseniaRepository reseniaRepository;
    private final ActividadRepository actividadRepository;
    private final AuditService auditService;
    private final NotificacionService notificacionService;

    public RechazarReseniaService(
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

    @Transactional
    public RechazarReseniaResponse rechazar(UUID id, UUID actorId) {
        Resenia resenia = reseniaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Reseña no encontrada."));
        UUID actividadId = resenia.getClase().getActividad().getId();
        UUID alumnoId = resenia.getAlumno().getId();
        String contexto = "la clase de \"" + resenia.getClase().getActividad().getNombre()
                + "\" del " + NotificacionMensajes.formatFechaHora(resenia.getClase().getFechaHora());

        resenia.marcarBorrado();
        reseniaRepository.save(resenia);
        actividadRepository.recalcularRating(actividadId);

        notificacionService.notificar(
                alumnoId, TipoNotificacion.RESENIA_RECHAZADA, "Tu reseña sobre " + contexto + " no fue aprobada.", id);

        auditService.registrar(actorId, AuditAccion.RESENIA_RECHAZADA, "Resenia", id, null);

        return new RechazarReseniaResponse(id);
    }
}
