package com.activehub.usecases.desbloquearinstructorpenalizacion;

import com.activehub.domain.penalizacion.ComprobantePenalizacion;
import com.activehub.domain.penalizacion.ComprobantePenalizacionRepository;
import com.activehub.domain.penalizacion.EstadoComprobantePenalizacion;
import com.activehub.domain.penalizacion.EstadoPagoPenalizacion;
import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.security.PenalizacionVigenteGuard;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Desbloquear instructor" (V36): el administrador aprueba el comprobante de pago de una multa.
 *
 * <p>La multa queda PAGADA y deja de bloquear. Si ademas tiene una suspension vigente, sigue
 * suspendido hasta que termine o hasta que un administrador la levante: pagar la multa no
 * acorta la suspension. Solo se aprueba un comprobante que el instructor envio: sin comprobante
 * no hay nada que verificar.
 */
@Service
public class DesbloquearInstructorPenalizacionService {

    private final PenalizacionRepository penalizacionRepository;
    private final ComprobantePenalizacionRepository comprobantePenalizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PenalizacionVigenteGuard penalizacionVigenteGuard;
    private final NotificacionService notificacionService;
    private final AuditService auditService;
    private final Clock clock;

    public DesbloquearInstructorPenalizacionService(
            PenalizacionRepository penalizacionRepository,
            ComprobantePenalizacionRepository comprobantePenalizacionRepository,
            UsuarioRepository usuarioRepository,
            PenalizacionVigenteGuard penalizacionVigenteGuard,
            NotificacionService notificacionService,
            AuditService auditService,
            Clock clock) {
        this.penalizacionRepository = penalizacionRepository;
        this.comprobantePenalizacionRepository = comprobantePenalizacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.penalizacionVigenteGuard = penalizacionVigenteGuard;
        this.notificacionService = notificacionService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public DesbloquearInstructorPenalizacionResponse desbloquear(UUID penalizacionId, UUID actorId) {
        Penalizacion multa = penalizacionRepository.findById(penalizacionId)
                .orElseThrow(() -> new NoEncontradoException("Penalización no encontrada."));
        if (multa.getEstadoPago() != EstadoPagoPenalizacion.EN_REVISION) {
            throw new ValidacionException("Esta multa no tiene un comprobante esperando revisión.");
        }
        ComprobantePenalizacion comprobante = comprobantePenalizacionRepository
                .findFirstByPenalizacionIdAndEstadoOrderByCreatedAtDesc(
                        multa.getId(), EstadoComprobantePenalizacion.EN_REVISION)
                .orElseThrow(() -> new ValidacionException("Esta multa no tiene un comprobante esperando revisión."));

        var ahora = clock.instant();
        var admin = usuarioRepository.getReferenceById(actorId);
        comprobante.setEstado(EstadoComprobantePenalizacion.APROBADO);
        comprobante.setRevisadoPor(admin);
        comprobante.setRevisadoAt(ahora);
        comprobantePenalizacionRepository.save(comprobante);

        multa.setEstadoPago(EstadoPagoPenalizacion.PAGADA);
        multa.setPagoAprobadoPor(admin);
        multa.setPagoAprobadoAt(ahora);
        penalizacionRepository.save(multa);

        auditService.registrar(actorId, AuditAccion.MULTA_PAGADA, "Penalizacion", multa.getId(), null);

        UUID instructorId = multa.getUsuario().getId();
        LocalDate suspendidoHasta = penalizacionVigenteGuard.suspensionVigente(instructorId)
                .map(Penalizacion::getFechaFin)
                .orElse(null);
        boolean otraMulta = penalizacionVigenteGuard.tieneMultaPendiente(instructorId);
        String mensaje = "La administración aprobó el comprobante de pago de tu multa.";
        if (suspendidoHasta != null) {
            mensaje += " Tu suspensión sigue vigente hasta el " + suspendidoHasta + ".";
        } else if (otraMulta) {
            mensaje += " Todavía tenés otra multa pendiente de pago.";
        } else {
            mensaje += " Ya podés volver a operar con normalidad.";
        }
        notificacionService.notificar(instructorId, TipoNotificacion.MULTA_PAGADA, mensaje, multa.getId(),
                Destino.penalizacion(multa.getId()));

        return new DesbloquearInstructorPenalizacionResponse(multa.getId(), multa.getEstadoPago().name(), suspendidoHasta);
    }
}
