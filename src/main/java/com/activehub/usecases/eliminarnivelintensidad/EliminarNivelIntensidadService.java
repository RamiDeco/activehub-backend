package com.activehub.usecases.eliminarnivelintensidad;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.NivelIntensidadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NivelIntensidadEnUsoException;
import com.activehub.shared.error.NoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E4Ad-HU05 criterios 6 y 7: con actividades asociadas se bloquea, porque la integridad
 * manda sobre la baja; sin ellas es baja lógica, nunca DELETE físico (RN-13).
 */
@Service
public class EliminarNivelIntensidadService {

    private final NivelIntensidadRepository nivelIntensidadRepository;
    private final ActividadRepository actividadRepository;
    private final AuditService auditService;

    public EliminarNivelIntensidadService(
            NivelIntensidadRepository nivelIntensidadRepository,
            ActividadRepository actividadRepository,
            AuditService auditService
    ) {
        this.nivelIntensidadRepository = nivelIntensidadRepository;
        this.actividadRepository = actividadRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void eliminar(UUID id, UUID actorId) {
        NivelIntensidad nivel = nivelIntensidadRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Nivel de intensidad no encontrado."));

        if (actividadRepository.existsByNivelIntensidadIdAndDeletedFalse(id)) {
            throw new NivelIntensidadEnUsoException();
        }

        nivel.marcarBorrado();
        nivelIntensidadRepository.save(nivel);

        auditService.registrar(actorId, AuditAccion.NIVEL_INTENSIDAD_ELIMINADO, "NivelIntensidad", id, null);
    }
}
