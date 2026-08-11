package com.activehub.usecases.actualizarclase;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActualizarClaseService {

    private final ClaseRepository claseRepository;
    private final AuditService auditService;

    public ActualizarClaseService(ClaseRepository claseRepository, AuditService auditService) {
        this.claseRepository = claseRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ActualizarClaseResponse actualizar(UUID id, ActualizarClaseRequest request, UUID instructorId) {
        Clase clase = claseRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Clase no encontrada."));

        if (!clase.getActividad().getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés modificar una clase que no te pertenece.");
        }

        if (request.cuposMax() < clase.getCuposOcupados()) {
            throw new ValidacionException(
                    "Los cupos máximos no pueden ser menores a los cupos ya ocupados.",
                    Map.of("cuposMax", "No puede ser menor a los cupos ya ocupados (" + clase.getCuposOcupados() + ")."));
        }

        clase.setFechaHora(request.fechaHora());
        clase.setCuposMax(request.cuposMax());
        claseRepository.save(clase);

        auditService.registrar(instructorId, AuditAccion.CLASE_ACTUALIZADA, "Clase", clase.getId(), null);

        return new ActualizarClaseResponse(
                clase.getId(), clase.getActividad().getId(), clase.getFechaHora(), clase.getEstado().name(),
                clase.getCuposMax(), clase.getCuposOcupados());
    }
}
