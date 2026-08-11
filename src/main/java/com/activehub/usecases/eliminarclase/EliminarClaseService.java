package com.activehub.usecases.eliminarclase;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarClaseService {

    private final ClaseRepository claseRepository;
    private final AuditService auditService;

    public EliminarClaseService(ClaseRepository claseRepository, AuditService auditService) {
        this.claseRepository = claseRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void eliminar(UUID id, UUID instructorId) {
        Clase clase = claseRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Clase no encontrada."));

        if (!clase.getActividad().getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés eliminar una clase que no te pertenece.");
        }

        clase.marcarBorrado();
        claseRepository.save(clase);

        auditService.registrar(instructorId, AuditAccion.CLASE_ELIMINADA, "Clase", id, null);
    }
}
