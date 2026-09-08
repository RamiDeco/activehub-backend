package com.activehub.usecases.liberarpagos;

import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.denuncia.EstadoDenuncia;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.inscripcion.VentanaPagos;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.payments.PaymentGateway;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RN-04, segunda mitad: acredita al instructor los pagos Retenidos de clases ya
 * Finalizadas una vez transcurrido el periodo de denuncias.
 *
 * <p>Un pago se libera solo si se cumplen las cuatro condiciones:
 * <ol>
 *   <li>el pago esta Retenido (los Efectivo se cobran mano a mano y los Cancelado ya se reintegraron);</li>
 *   <li>la inscripcion sigue Inscripto (si el alumno cancelo, el pago ya paso a Cancelado);</li>
 *   <li>la clase esta Finalizada y pasaron {@link VentanaPagos#PERIODO_DENUNCIAS} desde su horario;</li>
 *   <li>no hay ninguna denuncia abierta sobre esa clase.</li>
 * </ol>
 *
 * <p>Si hay una denuncia sin resolver el pago se queda Retenido: cuando el admin la
 * resuelve, o bien reintegra (y el pago pasa a Cancelado) o bien la desestima, y en la
 * siguiente corrida este servicio lo libera.
 */
@Service
public class LiberarPagosService {

    private final PagoRepository pagoRepository;
    private final DenunciaRepository denunciaRepository;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;
    private final Clock clock;

    public LiberarPagosService(
            PagoRepository pagoRepository,
            DenunciaRepository denunciaRepository,
            PaymentGateway paymentGateway,
            AuditService auditService,
            Clock clock
    ) {
        this.pagoRepository = pagoRepository;
        this.denunciaRepository = denunciaRepository;
        this.paymentGateway = paymentGateway;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public int liberar() {
        Instant limite = clock.instant().minus(VentanaPagos.PERIODO_DENUNCIAS);
        int liberados = 0;

        for (Pago pago : pagoRepository.findByEstadoConClase(EstadoPago.Retenido)) {
            Inscripcion inscripcion = pago.getInscripcion();
            var clase = inscripcion.getClase();

            if (inscripcion.getEstado() != EstadoInscripcion.INSCRIPTO) {
                continue;
            }
            if (clase.getEstado() != EstadoClase.Finalizada) {
                continue;
            }
            if (!clase.getFechaHora().isBefore(limite)) {
                continue;
            }
            if (denunciaRepository.existsByClaseIdAndEstadoNot(clase.getId(), EstadoDenuncia.RESUELTA)) {
                continue;
            }

            paymentGateway.confirmarPago(pago.getReferenciaExterna());
            pago.setEstado(EstadoPago.Liberado);
            pagoRepository.save(pago);

            // actorId=null: lo dispara el sistema, no una persona. RN-14 pide auditar los
            // movimientos de dinero, que hasta ahora no dejaban rastro propio.
            auditService.registrar(null, AuditAccion.PAGO_LIBERADO, "Pago", pago.getId(), null);
            liberados++;
        }

        return liberados;
    }
}
