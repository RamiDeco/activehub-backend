package com.activehub.usecases.actualizarnivelintensidad;

import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.NivelIntensidadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NivelIntensidadDuplicadoException;
import com.activehub.shared.error.NoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** E4Ad-HU05 criterios 3, 4 y 5. */
@Service
public class ActualizarNivelIntensidadService {

    private final NivelIntensidadRepository nivelIntensidadRepository;
    private final AuditService auditService;

    public ActualizarNivelIntensidadService(
            NivelIntensidadRepository nivelIntensidadRepository, AuditService auditService) {
        this.nivelIntensidadRepository = nivelIntensidadRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ActualizarNivelIntensidadResponse actualizar(
            UUID id, ActualizarNivelIntensidadRequest request, UUID actorId) {
        NivelIntensidad nivel = nivelIntensidadRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Nivel de intensidad no encontrado."));

        // El propio nivel no cuenta como duplicado: cambiarle sólo la capitalización o
        // editarle la descripción sin tocar el nombre tiene que poder guardarse.
        nivelIntensidadRepository.findByNombreIgnoreCaseAndDeletedFalse(request.nombre().trim())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new NivelIntensidadDuplicadoException();
                });

        nivel.setNombre(request.nombre().trim());
        nivel.setDescripcion(request.descripcion().trim());
        nivelIntensidadRepository.save(nivel);

        auditService.registrar(
                actorId, AuditAccion.NIVEL_INTENSIDAD_ACTUALIZADO, "NivelIntensidad", nivel.getId(), null);

        return new ActualizarNivelIntensidadResponse(nivel.getId(), nivel.getNombre(), nivel.getDescripcion());
    }
}
