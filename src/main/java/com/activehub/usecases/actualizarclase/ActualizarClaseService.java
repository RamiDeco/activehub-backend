package com.activehub.usecases.actualizarclase;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import java.util.Map;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActualizarClaseService {

    private final ClaseRepository claseRepository;
    private final AuditService auditService;

    private final InstructorVerificadoGuard instructorVerificadoGuard;

    public ActualizarClaseService(
            ClaseRepository claseRepository,
            AuditService auditService,
            InstructorVerificadoGuard instructorVerificadoGuard
    ) {
        this.claseRepository = claseRepository;
        this.auditService = auditService;
        this.instructorVerificadoGuard = instructorVerificadoGuard;
    }

    @Transactional
    public ActualizarClaseResponse actualizar(UUID id, ActualizarClaseRequest request, UUID instructorId) {
        instructorVerificadoGuard.exigirVerificado(instructorId, "editar clases");
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

        // Mismas dos reglas de horario que al crear (E2I-HU06 criterios 4 y 8): editar una
        // clase no puede ser una puerta de atrás para dejarla con fin <= inicio o pisando otra.
        if (!request.horaFin().isAfter(request.fechaHora())) {
            throw new ValidacionException(
                    "La hora de fin debe ser posterior a la hora de inicio.",
                    Map.of("horaFin", "La hora de fin debe ser posterior a la hora de inicio."));
        }
        if (claseRepository.existeSolapamiento(
                clase.getActividad().getId(), request.fechaHora(), request.horaFin(), clase.getId())) {
            throw new ValidacionException(
                    "Ya existe una clase en ese horario. Modificá la fecha o el horario antes de continuar.");
        }

        clase.setFechaHora(request.fechaHora());
        clase.setHoraFin(request.horaFin());
        clase.setCuposMax(request.cuposMax());
        claseRepository.save(clase);

        auditService.registrar(instructorId, AuditAccion.CLASE_ACTUALIZADA, "Clase", clase.getId(), null);

        return new ActualizarClaseResponse(
                clase.getId(), clase.getActividad().getId(), clase.getFechaHora(), clase.getHoraFin(),
                clase.getEstado().name(), clase.getCuposMax(), clase.getCuposOcupados());
    }
}
