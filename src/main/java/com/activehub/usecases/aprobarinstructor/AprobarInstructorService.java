package com.activehub.usecases.aprobarinstructor;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AprobarInstructorService {

    private final PerfilInstructorRepository perfilInstructorRepository;
    private final AuditService auditService;
    private final NotificacionService notificacionService;

    public AprobarInstructorService(
            PerfilInstructorRepository perfilInstructorRepository, AuditService auditService, NotificacionService notificacionService
    ) {
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public AprobarInstructorResponse aprobar(UUID usuarioId, UUID actorId) {
        var perfil = perfilInstructorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Instructor no encontrado."));

        perfil.setEstadoVerificacion(EstadoVerificacion.APROBADO);
        perfil.setMotivoRechazo(null);
        perfilInstructorRepository.save(perfil);

        notificacionService.notificar(
                usuarioId, TipoNotificacion.INSTRUCTOR_APROBADO,
                "Tu perfil de instructor fue aprobado. Ya podés crear actividades y clases.", usuarioId);

        auditService.registrar(actorId, AuditAccion.INSTRUCTOR_VALIDADO, "PerfilInstructor", usuarioId, null);

        return new AprobarInstructorResponse(usuarioId, perfil.getEstadoVerificacion().name());
    }
}
