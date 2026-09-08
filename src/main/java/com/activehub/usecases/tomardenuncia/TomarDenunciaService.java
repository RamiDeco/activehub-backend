package com.activehub.usecases.tomardenuncia;

import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.denuncia.EstadoDenuncia;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E4Ad-HU07 criterio 3: cuando el admin abre una denuncia Pendiente para revisarla, pasa a
 * En Auditoria y el evento queda en trazabilidad.
 *
 * <p>Es la transicion del medio de {@code EstadoDenuncia} (Pendiente -> En Auditoria ->
 * Resuelta). Hasta ahora {@code EN_AUDITORIA} estaba declarado en el enum y permitido por el
 * CHECK de la migracion, pero no lo escribia nadie: la denuncia saltaba de Pendiente a
 * Resuelta y el alumno nunca veia progresar su reporte.
 *
 * <p>Es idempotente: tomar una denuncia que ya esta En Auditoria no falla ni vuelve a auditar,
 * porque el admin puede abrir la misma fila varias veces.
 */
@Service
public class TomarDenunciaService {

    private final DenunciaRepository denunciaRepository;
    private final AuditService auditService;

    public TomarDenunciaService(DenunciaRepository denunciaRepository, AuditService auditService) {
        this.denunciaRepository = denunciaRepository;
        this.auditService = auditService;
    }

    @Transactional
    public TomarDenunciaResponse tomar(UUID id, UUID actorId) {
        Denuncia denuncia = denunciaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Denuncia no encontrada."));

        if (denuncia.getEstado() == EstadoDenuncia.RESUELTA) {
            // El estado solo avanza: una denuncia resuelta no vuelve a En Auditoria.
            throw new ValidacionException("Esta denuncia ya fue resuelta.");
        }

        if (denuncia.getEstado() == EstadoDenuncia.PENDIENTE) {
            denuncia.setEstado(EstadoDenuncia.EN_AUDITORIA);
            denunciaRepository.save(denuncia);
            auditService.registrar(actorId, AuditAccion.DENUNCIA_EN_AUDITORIA, "Denuncia", id, null);
        }

        return new TomarDenunciaResponse(denuncia.getId(), denuncia.getEstado().getEtiqueta());
    }
}
