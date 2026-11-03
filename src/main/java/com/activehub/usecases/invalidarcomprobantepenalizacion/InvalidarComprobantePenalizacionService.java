package com.activehub.usecases.invalidarcomprobantepenalizacion;

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
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Notificar invalidez" (V36): el comprobante no sirve. Queda INVALIDO con su motivo (es
 * historial: no se borra), la multa vuelve a PENDIENTE_PAGO y el instructor puede mandar otro.
 * Sigue bloqueado mientras tanto.
 */
@Service
public class InvalidarComprobantePenalizacionService {

    private final PenalizacionRepository penalizacionRepository;
    private final ComprobantePenalizacionRepository comprobantePenalizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final AuditService auditService;
    private final Clock clock;

    public InvalidarComprobantePenalizacionService(
            PenalizacionRepository penalizacionRepository,
            ComprobantePenalizacionRepository comprobantePenalizacionRepository,
            UsuarioRepository usuarioRepository,
            NotificacionService notificacionService,
            AuditService auditService,
            Clock clock) {
        this.penalizacionRepository = penalizacionRepository;
        this.comprobantePenalizacionRepository = comprobantePenalizacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public InvalidarComprobantePenalizacionResponse invalidar(
            UUID penalizacionId, InvalidarComprobantePenalizacionRequest request, UUID actorId) {
        // El body es obligatorio pero el motivo se vuelve a mirar aca, como en rechazarinstructor.
        String motivo = request == null || request.motivo() == null ? "" : request.motivo().trim();
        if (motivo.isEmpty()) {
            throw new ValidacionException("Indicá por qué el comprobante no es válido.");
        }

        Penalizacion multa = penalizacionRepository.findById(penalizacionId)
                .orElseThrow(() -> new NoEncontradoException("Penalización no encontrada."));
        if (multa.getEstadoPago() != EstadoPagoPenalizacion.EN_REVISION) {
            throw new ValidacionException("Esta multa no tiene un comprobante esperando revisión.");
        }
        ComprobantePenalizacion comprobante = comprobantePenalizacionRepository
                .findFirstByPenalizacionIdAndEstadoOrderByCreatedAtDesc(
                        multa.getId(), EstadoComprobantePenalizacion.EN_REVISION)
                .orElseThrow(() -> new ValidacionException("Esta multa no tiene un comprobante esperando revisión."));

        comprobante.setEstado(EstadoComprobantePenalizacion.INVALIDO);
        comprobante.setMotivoInvalidez(motivo);
        comprobante.setRevisadoPor(usuarioRepository.getReferenceById(actorId));
        comprobante.setRevisadoAt(clock.instant());
        comprobantePenalizacionRepository.save(comprobante);

        multa.setEstadoPago(EstadoPagoPenalizacion.PENDIENTE_PAGO);
        penalizacionRepository.save(multa);

        auditService.registrar(actorId, AuditAccion.COMPROBANTE_PENALIZACION_INVALIDO, "Penalizacion", multa.getId(),
                "INVALIDO · " + motivo);

        notificacionService.notificar(
                multa.getUsuario().getId(), TipoNotificacion.COMPROBANTE_PENALIZACION_INVALIDO,
                "La administración marcó inválido el comprobante de pago de tu multa. Motivo: " + motivo
                        + " Cargá un comprobante nuevo desde \"Pago de penalización\".",
                multa.getId(), Destino.penalizacion(multa.getId()));

        return new InvalidarComprobantePenalizacionResponse(multa.getId(), multa.getEstadoPago().name());
    }
}
