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
import com.activehub.shared.payments.PaymentGateway;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelarClaseService {

    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;

    public CancelarClaseService(
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            PagoRepository pagoRepository,
            PaymentGateway paymentGateway,
            AuditService auditService
    ) {
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.paymentGateway = paymentGateway;
        this.auditService = auditService;
    }

    @Transactional
    public CancelarClaseResponse cancelar(UUID claseId, UUID instructorId) {
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

        for (Inscripcion inscripcion : inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA)) {
            Pago pago = inscripcion.getPago();
            if (pago != null && pago.getEstado() == EstadoPago.Retenido) {
                paymentGateway.cancelarPago(pago.getReferenciaExterna());
                pago.setEstado(EstadoPago.Cancelado);
                pagoRepository.save(pago);
            }
            inscripcion.setEstado(EstadoInscripcion.CANCELADA);
            inscripcionRepository.save(inscripcion);
        }

        clase.setEstado(EstadoClase.Cancelada);
        claseRepository.save(clase);

        auditService.registrar(instructorId, AuditAccion.CLASE_CANCELADA, "Clase", clase.getId(), null);

        return new CancelarClaseResponse(clase.getId(), clase.getEstado().name());
    }
}
