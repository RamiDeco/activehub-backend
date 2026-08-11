package com.activehub.usecases.rechazarinstructor;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RechazarInstructorService {

    private final PerfilInstructorRepository perfilInstructorRepository;
    private final AuditService auditService;

    public RechazarInstructorService(PerfilInstructorRepository perfilInstructorRepository, AuditService auditService) {
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.auditService = auditService;
    }

    @Transactional
    public RechazarInstructorResponse rechazar(UUID usuarioId, RechazarInstructorRequest request, UUID actorId) {
        var perfil = perfilInstructorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Instructor no encontrado."));

        String motivo = request.motivo() != null && !request.motivo().isBlank() ? request.motivo().trim() : null;
        perfil.setEstadoVerificacion(EstadoVerificacion.RECHAZADO);
        perfil.setMotivoRechazo(motivo);
        perfilInstructorRepository.save(perfil);

        auditService.registrar(actorId, AuditAccion.INSTRUCTOR_RECHAZADO, "PerfilInstructor", usuarioId, motivo);

        return new RechazarInstructorResponse(usuarioId, perfil.getEstadoVerificacion().name(), motivo);
    }
}
