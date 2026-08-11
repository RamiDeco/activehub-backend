package com.activehub.usecases.eliminaractividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarActividadService {

    private final ActividadRepository actividadRepository;
    private final AuditService auditService;

    public EliminarActividadService(ActividadRepository actividadRepository, AuditService auditService) {
        this.actividadRepository = actividadRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void eliminar(UUID id, UUID instructorId) {
        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));

        if (!actividad.getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés eliminar una actividad que no te pertenece.");
        }

        actividad.marcarBorrado();
        actividadRepository.save(actividad);

        auditService.registrar(instructorId, AuditAccion.ACTIVIDAD_ELIMINADA, "Actividad", id, null);
    }
}
