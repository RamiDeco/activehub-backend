package com.activehub.usecases.rechazarinstructor;

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
public class RechazarInstructorService {

    private final PerfilInstructorRepository perfilInstructorRepository;
    private final AuditService auditService;
    private final NotificacionService notificacionService;

    public RechazarInstructorService(
            PerfilInstructorRepository perfilInstructorRepository, AuditService auditService, NotificacionService notificacionService
    ) {
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public RechazarInstructorResponse rechazar(UUID usuarioId, RechazarInstructorRequest request, UUID actorId) {
        var perfil = perfilInstructorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Instructor no encontrado."));

        String motivo = request.motivo() != null && !request.motivo().isBlank() ? request.motivo().trim() : null;
        perfil.setEstadoVerificacion(EstadoVerificacion.RECHAZADO);
        perfil.setMotivoRechazo(motivo);
        perfilInstructorRepository.save(perfil);

        String mensaje = "Tu perfil de instructor fue rechazado." + (motivo != null ? " Motivo: " + motivo : "");
        notificacionService.notificar(usuarioId, TipoNotificacion.INSTRUCTOR_RECHAZADO, mensaje, usuarioId);

        auditService.registrar(actorId, AuditAccion.INSTRUCTOR_RECHAZADO, "PerfilInstructor", usuarioId, motivo);

        return new RechazarInstructorResponse(usuarioId, perfil.getEstadoVerificacion().name(), motivo);
    }
}
