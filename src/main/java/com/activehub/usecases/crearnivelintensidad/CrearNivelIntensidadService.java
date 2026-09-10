package com.activehub.usecases.crearnivelintensidad;

import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.NivelIntensidadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NivelIntensidadDuplicadoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** E4Ad-HU05 criterios 2, 3 y 4. */
@Service
public class CrearNivelIntensidadService {

    private final NivelIntensidadRepository nivelIntensidadRepository;
    private final AuditService auditService;

    public CrearNivelIntensidadService(
            NivelIntensidadRepository nivelIntensidadRepository, AuditService auditService) {
        this.nivelIntensidadRepository = nivelIntensidadRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CrearNivelIntensidadResponse crear(CrearNivelIntensidadRequest request, UUID actorId) {
        String nombre = request.nombre().trim();
        // Criterio 4: el duplicado se consulta contra la base, no contra una lista en memoria.
        if (nivelIntensidadRepository.existsByNombreIgnoreCaseAndDeletedFalse(nombre)) {
            throw new NivelIntensidadDuplicadoException();
        }

        NivelIntensidad nivel = new NivelIntensidad();
        nivel.setNombre(nombre);
        nivel.setDescripcion(request.descripcion().trim());
        nivel = nivelIntensidadRepository.saveAndFlush(nivel);

        auditService.registrar(
                actorId, AuditAccion.NIVEL_INTENSIDAD_CREADO, "NivelIntensidad", nivel.getId(), null);

        return new CrearNivelIntensidadResponse(nivel.getId(), nivel.getNombre(), nivel.getDescripcion());
    }
}
