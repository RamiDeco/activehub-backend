package com.activehub.usecases.eliminarresenia;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarReseniaService {

    private final ReseniaRepository reseniaRepository;
    private final ActividadRepository actividadRepository;
    private final AuditService auditService;

    public EliminarReseniaService(
            ReseniaRepository reseniaRepository, ActividadRepository actividadRepository, AuditService auditService
    ) {
        this.reseniaRepository = reseniaRepository;
        this.actividadRepository = actividadRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void eliminar(UUID id, UUID alumnoId) {
        Resenia resenia = reseniaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Reseña no encontrada."));

        if (!resenia.getAlumno().getId().equals(alumnoId)) {
            throw new SinPermisoException("No podés eliminar una reseña que no te pertenece.");
        }

        UUID actividadId = resenia.getClase().getActividad().getId();
        resenia.marcarBorrado();
        reseniaRepository.save(resenia);
        actividadRepository.recalcularRating(actividadId);

        auditService.registrar(alumnoId, AuditAccion.RESENIA_ELIMINADA, "Resenia", id, null);
    }
}
