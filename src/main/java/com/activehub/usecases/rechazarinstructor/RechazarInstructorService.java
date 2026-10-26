package com.activehub.usecases.rechazarinstructor;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RechazarInstructorService {

    private final PerfilInstructorRepository perfilInstructorRepository;
    private final AuditService auditService;
    private final NotificacionService notificacionService;
    private final Clock clock;

    public RechazarInstructorService(
            PerfilInstructorRepository perfilInstructorRepository,
            AuditService auditService,
            NotificacionService notificacionService,
            Clock clock
    ) {
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
        this.clock = clock;
    }

    @Transactional
    public RechazarInstructorResponse rechazar(UUID usuarioId, RechazarInstructorRequest request, UUID actorId) {
        var perfil = perfilInstructorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Instructor no encontrado."));

        // También acá y no sólo en el Request: el Controller acepta el cuerpo vacío (required = false).
        if (request.motivo() == null || request.motivo().isBlank()) {
            throw new ValidacionException(
                    "Escribí el motivo del rechazo: es lo que va a leer el instructor para corregir su solicitud.");
        }
        String motivo = request.motivo().trim();
        perfil.setEstadoVerificacion(EstadoVerificacion.RECHAZADO);
        perfil.setMotivoRechazo(motivo);
        // Desde acá cuenta la documentación "nueva" que se le pide para volver a postularse.
        perfil.setRechazadoAt(Instant.now(clock));
        perfilInstructorRepository.save(perfil);
        String mensaje = "Tu perfil de instructor fue rechazado. Motivo: " + motivo
                + " Subí la documentación que falta desde \"Mis datos\" y volvé a postularte.";
        notificacionService.notificar(
                usuarioId, TipoNotificacion.INSTRUCTOR_RECHAZADO, mensaje, usuarioId, Destino.perfilInstructor(usuarioId));

        auditService.registrar(actorId, AuditAccion.INSTRUCTOR_RECHAZADO, "PerfilInstructor", usuarioId, motivo);

        return new RechazarInstructorResponse(usuarioId, perfil.getEstadoVerificacion().name(), motivo);
    }
}
