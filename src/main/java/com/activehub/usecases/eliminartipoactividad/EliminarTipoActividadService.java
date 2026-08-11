package com.activehub.usecases.eliminartipoactividad;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.TipoActividadEnUsoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarTipoActividadService {

    private final TipoActividadRepository tipoActividadRepository;
    private final ActividadRepository actividadRepository;
    private final AuditService auditService;

    public EliminarTipoActividadService(
            TipoActividadRepository tipoActividadRepository,
            ActividadRepository actividadRepository,
            AuditService auditService
    ) {
        this.tipoActividadRepository = tipoActividadRepository;
        this.actividadRepository = actividadRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void eliminar(UUID id, UUID actorId) {
        TipoActividad tipo = tipoActividadRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Tipo de actividad no encontrado."));

        if (actividadRepository.existsByTipoActividadIdAndDeletedFalse(id)) {
            throw new TipoActividadEnUsoException();
        }

        tipo.marcarBorrado();
        tipoActividadRepository.save(tipo);

        auditService.registrar(actorId, AuditAccion.TIPO_ACTIVIDAD_ELIMINADO, "TipoActividad", id, null);
    }
}
