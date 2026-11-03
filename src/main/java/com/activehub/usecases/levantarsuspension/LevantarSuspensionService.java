package com.activehub.usecases.levantarsuspension;

import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.security.PenalizacionVigenteGuard;
import com.activehub.shared.time.Zonas;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Desbloquear" una suspension temporal antes de que termine (V36, decision del usuario: el
 * administrador puede hacerlo en cualquier momento y por cualquier motivo, que es obligatorio
 * y queda auditado).
 *
 * <p>No deshace nada de lo que la suspension ya hizo: las clases que se cancelaron al
 * aplicarla siguen canceladas y sus reintegros siguen hechos. Tampoco toca una multa que
 * acompane a la suspension: esa se libera aprobando su comprobante.
 */
@Service
public class LevantarSuspensionService {

    private final PenalizacionRepository penalizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PenalizacionVigenteGuard penalizacionVigenteGuard;
    private final NotificacionService notificacionService;
    private final AuditService auditService;
    private final Clock clock;

    public LevantarSuspensionService(
            PenalizacionRepository penalizacionRepository,
            UsuarioRepository usuarioRepository,
            PenalizacionVigenteGuard penalizacionVigenteGuard,
            NotificacionService notificacionService,
            AuditService auditService,
            Clock clock) {
        this.penalizacionRepository = penalizacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.penalizacionVigenteGuard = penalizacionVigenteGuard;
        this.notificacionService = notificacionService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public LevantarSuspensionResponse levantar(UUID penalizacionId, LevantarSuspensionRequest request, UUID actorId) {
        String motivo = request == null || request.motivo() == null ? "" : request.motivo().trim();
        if (motivo.isEmpty()) {
            throw new ValidacionException("Indicá por qué levantás la suspensión.");
        }

        Penalizacion suspension = penalizacionRepository.findById(penalizacionId)
                .orElseThrow(() -> new NoEncontradoException("Penalización no encontrada."));
        if (suspension.getTipo() != TipoPenalizacion.SUSPENSION_TEMPORAL) {
            throw new ValidacionException("Solo se puede levantar una suspensión temporal.");
        }
        if (suspension.getLevantadaAt() != null) {
            throw new ValidacionException("Esta suspensión ya fue levantada.");
        }
        LocalDate hoy = LocalDate.now(clock.withZone(Zonas.AR));
        if (suspension.getFechaFin() == null || suspension.getFechaFin().isBefore(hoy)) {
            throw new ValidacionException("Esta suspensión ya terminó.");
        }

        suspension.setLevantadaAt(clock.instant());
        suspension.setLevantadaPor(usuarioRepository.getReferenceById(actorId));
        suspension.setMotivoLevantamiento(motivo);
        penalizacionRepository.save(suspension);

        auditService.registrar(actorId, AuditAccion.SUSPENSION_LEVANTADA, "Penalizacion", suspension.getId(),
                "ADMIN · " + motivo);

        UUID instructorId = suspension.getUsuario().getId();
        boolean multaPendiente = penalizacionVigenteGuard.tieneMultaPendiente(instructorId);
        boolean otraSuspension = penalizacionVigenteGuard.suspensionVigente(instructorId).isPresent();
        String mensaje = "La administración levantó tu suspensión.";
        if (multaPendiente) {
            mensaje += " Todavía tenés una multa pendiente de pago: cargá el comprobante desde \"Pago de penalización\".";
        } else if (!otraSuspension) {
            mensaje += " Ya podés volver a operar con normalidad.";
        }
        notificacionService.notificar(instructorId, TipoNotificacion.SUSPENSION_LEVANTADA, mensaje,
                suspension.getId(), Destino.penalizacion(suspension.getId()));

        return new LevantarSuspensionResponse(suspension.getId(), suspension.getLevantadaAt(), multaPendiente);
    }
}
