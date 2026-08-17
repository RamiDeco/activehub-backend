package com.activehub.usecases.rechazarresenia;

import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RechazarReseniaService {

    private final ReseniaRepository reseniaRepository;
    private final AuditService auditService;

    public RechazarReseniaService(ReseniaRepository reseniaRepository, AuditService auditService) {
        this.reseniaRepository = reseniaRepository;
        this.auditService = auditService;
    }

    @Transactional
    public RechazarReseniaResponse rechazar(UUID id, UUID actorId) {
        Resenia resenia = reseniaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Reseña no encontrada."));

        resenia.marcarBorrado();
        reseniaRepository.save(resenia);

        auditService.registrar(actorId, AuditAccion.RESENIA_RECHAZADA, "Resenia", id, null);

        return new RechazarReseniaResponse(id);
    }
}
