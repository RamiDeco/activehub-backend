package com.activehub.usecases.aprobarresenia;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AprobarReseniaService {

    private final ReseniaRepository reseniaRepository;
    private final ActividadRepository actividadRepository;
    private final AuditService auditService;

    public AprobarReseniaService(
            ReseniaRepository reseniaRepository, ActividadRepository actividadRepository, AuditService auditService
    ) {
        this.reseniaRepository = reseniaRepository;
        this.actividadRepository = actividadRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AprobarReseniaResponse aprobar(UUID id, UUID actorId) {
        Resenia resenia = reseniaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Reseña no encontrada."));

        resenia.setEnModeracion(false);
        reseniaRepository.save(resenia);
        actividadRepository.recalcularRating(resenia.getClase().getActividad().getId());

        auditService.registrar(actorId, AuditAccion.RESENIA_APROBADA, "Resenia", id, null);

        return new AprobarReseniaResponse(resenia.getId(), resenia.isEnModeracion());
    }
}
