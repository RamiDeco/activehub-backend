package com.activehub.usecases.cancelarclase;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.payments.PaymentGateway;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelarClaseService {

    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;
    private final PaymentGateway paymentGateway;
    private final NotificacionService notificacionService;
    private final AuditService auditService;

    private final InstructorVerificadoGuard instructorVerificadoGuard;

    public CancelarClaseService(
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            PagoRepository pagoRepository,
            PaymentGateway paymentGateway,
            NotificacionService notificacionService,
            AuditService auditService,
            InstructorVerificadoGuard instructorVerificadoGuard
    ) {
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.paymentGateway = paymentGateway;
        this.notificacionService = notificacionService;
        this.auditService = auditService;
        this.instructorVerificadoGuard = instructorVerificadoGuard;
    }

    @Transactional
    public CancelarClaseResponse cancelar(UUID claseId, UUID instructorId) {
        instructorVerificadoGuard.exigirVerificado(instructorId, "cancelar clases");
        Clase clase = claseRepository.findById(claseId)
                .orElseThrow(() -> new NoEncontradoException("Clase no encontrada."));

        if (!clase.getActividad().getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés cancelar una clase que no te pertenece.");
        }

        if (clase.getEstado() == EstadoClase.Cancelada) {
            throw new ValidacionException("Esta clase ya está cancelada.");
        }
        if (clase.getEstado() == EstadoClase.Finalizada) {
            throw new ValidacionException("No podés cancelar una clase que ya finalizó.");
        }

        String mensaje = "El instructor canceló la clase de \"" + clase.getActividad().getNombre()
                + "\" del " + NotificacionMensajes.formatFechaHora(clase.getFechaHora())
                + ". Tu inscripción fue cancelada.";
        for (Inscripcion inscripcion : inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA)) {
            Pago pago = inscripcion.getPago();
            String detallePago = "";
            // E2I-HU08 criterio 5: el reintegro alcanza a los pagos Retenidos *y* a los que
            // ya se cobraron en Efectivo; ambos quedan en Cancelado. Antes el efectivo no se
            // tocaba y el alumno seguía viendo su pago como válido para una clase que no existe.
            if (pago != null && pago.getEstado() == EstadoPago.Retenido) {
                paymentGateway.cancelarPago(pago.getReferenciaExterna());
                pago.setEstado(EstadoPago.Cancelado);
                pagoRepository.save(pago);
                detallePago = " Se reintegra el pago retenido.";
            } else if (pago != null && pago.getEstado() == EstadoPago.Efectivo) {
                // No hay gateway al que pedirle nada: la plata la cobró el instructor en mano.
                // El registro queda Cancelado y la devolución se arregla con él.
                pago.setEstado(EstadoPago.Cancelado);
                pagoRepository.save(pago);
                detallePago = " Coordiná con el instructor la devolución de lo que pagaste en efectivo.";
            }
            inscripcion.setEstado(EstadoInscripcion.CANCELADA);
            inscripcionRepository.save(inscripcion);

            notificacionService.notificar(
                    inscripcion.getAlumno().getId(), TipoNotificacion.CLASE_CANCELADA, mensaje + detallePago, clase.getId(),
                    Destino.clase(clase.getId()));
        }

        clase.setEstado(EstadoClase.Cancelada);
        claseRepository.save(clase);

        auditService.registrar(instructorId, AuditAccion.CLASE_CANCELADA, "Clase", clase.getId(), null);

        return new CancelarClaseResponse(clase.getId(), clase.getEstado().name());
    }
}
