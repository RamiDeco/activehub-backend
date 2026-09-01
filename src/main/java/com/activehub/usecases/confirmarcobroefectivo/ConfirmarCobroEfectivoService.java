package com.activehub.usecases.confirmarcobroefectivo;

import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmarCobroEfectivoService {

    private final InscripcionRepository inscripcionRepository;
    private final AuditService auditService;
    private final NotificacionService notificacionService;

    public ConfirmarCobroEfectivoService(
            InscripcionRepository inscripcionRepository, AuditService auditService, NotificacionService notificacionService
    ) {
        this.inscripcionRepository = inscripcionRepository;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public ConfirmarCobroEfectivoResponse confirmar(UUID id, UUID instructorId) {
        Inscripcion inscripcion = inscripcionRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Inscripción no encontrada."));

        if (!inscripcion.getClase().getActividad().getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés confirmar el cobro de una clase que no te pertenece.");
        }

        if (inscripcion.getEstado() != EstadoInscripcion.PAGO_PENDIENTE) {
            throw new ValidacionException("Esta inscripción no tiene un cobro en efectivo pendiente.");
        }

        inscripcion.setEstado(EstadoInscripcion.INSCRIPTO);
        inscripcionRepository.save(inscripcion);

        notificacionService.notificar(
                inscripcion.getAlumno().getId(),
                TipoNotificacion.COBRO_EFECTIVO_CONFIRMADO,
                "Confirmaron tu pago en efectivo de la clase de \"" + inscripcion.getClase().getActividad().getNombre()
                        + "\" del " + NotificacionMensajes.formatFechaHora(inscripcion.getClase().getFechaHora())
                        + ". Quedaste inscripto.",
                inscripcion.getId());

        auditService.registrar(instructorId, AuditAccion.COBRO_EFECTIVO_CONFIRMADO, "Inscripcion", inscripcion.getId(), null);

        return new ConfirmarCobroEfectivoResponse(inscripcion.getId(), inscripcion.getEstado().getEtiqueta());
    }
}
