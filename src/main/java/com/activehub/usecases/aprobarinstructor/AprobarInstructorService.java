package com.activehub.usecases.aprobarinstructor;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AprobarInstructorService {

    private final PerfilInstructorRepository perfilInstructorRepository;
    private final AuditService auditService;

    public AprobarInstructorService(PerfilInstructorRepository perfilInstructorRepository, AuditService auditService) {
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AprobarInstructorResponse aprobar(UUID usuarioId, UUID actorId) {
        var perfil = perfilInstructorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Instructor no encontrado."));

        perfil.setEstadoVerificacion(EstadoVerificacion.APROBADO);
        perfil.setMotivoRechazo(null);
        perfilInstructorRepository.save(perfil);

        auditService.registrar(actorId, AuditAccion.INSTRUCTOR_VALIDADO, "PerfilInstructor", usuarioId, null);

        return new AprobarInstructorResponse(usuarioId, perfil.getEstadoVerificacion().name());
    }
}
