package com.activehub.usecases.desvincularcuentamp;

import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructorRepository;
import com.activehub.domain.mercadopago.EstadoCuentaMercadoPago;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.CuentaMercadoPagoConPagosPendientesException;
import com.activehub.shared.error.NoEncontradoException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KAN-55: el instructor desconecta su cuenta de Mercado Pago. La fila queda en
 * {@code DESVINCULADA} y sin tokens (no se borra: si vuelve a vincular, se reutiliza). Desde
 * ese momento sus clases solo aceptan efectivo ({@code CuentaMercadoPagoGuard}).
 *
 * <ul>
 *   <li><b>Se bloquea con pagos pendientes en MP</b> (409): un pago {@code Retenido}, capturado
 *       o no, y uno {@code EnRevision} todavia se cobran, se reintegran o se consultan con el
 *       token del instructor. Borrarlo dejaria la plata de esos alumnos sin camino de vuelta.
 *       Es la misma condicion que {@link EstadoPago#reservadoEnMercadoPago()}.</li>
 *   <li>Una cuenta {@code TOKEN_VENCIDO} tambien se puede desvincular: es limpiar una
 *       vinculacion que ya no sirve.</li>
 *   <li>Desvincular una cuenta ya desvinculada no hace nada (DELETE idempotente) y no audita.</li>
 * </ul>
 */
@Service
public class DesvincularCuentaMPService {

    private static final List<EstadoPago> PENDIENTES_EN_MP = List.of(EstadoPago.Retenido, EstadoPago.EnRevision);

    private final CuentaMercadoPagoInstructorRepository cuentaRepository;
    private final PagoRepository pagoRepository;
    private final AuditService auditService;
    private final Clock clock;

    public DesvincularCuentaMPService(
            CuentaMercadoPagoInstructorRepository cuentaRepository,
            PagoRepository pagoRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.cuentaRepository = cuentaRepository;
        this.pagoRepository = pagoRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public void desvincular(UUID instructorId) {
        // Proyeccion: para saber el estado no hace falta descifrar los tokens.
        var cuenta = cuentaRepository.findResumenByInstructorId(instructorId)
                .orElseThrow(() -> new NoEncontradoException("No tenés una cuenta de Mercado Pago vinculada."));
        if (cuenta.getEstado() == EstadoCuentaMercadoPago.DESVINCULADA) {
            return;
        }

        long pendientes = pagoRepository.countDeMercadoPagoDeInstructorEn(instructorId, PENDIENTES_EN_MP);
        if (pendientes > 0) {
            throw new CuentaMercadoPagoConPagosPendientesException(pendientes);
        }

        cuentaRepository.desvincular(instructorId, EstadoCuentaMercadoPago.DESVINCULADA, clock.instant());

        auditService.registrar(instructorId, AuditAccion.MERCADOPAGO_DESVINCULADA, "CuentaMercadoPagoInstructor", cuenta.getId(),
                "mp_user_id=" + cuenta.getMpUserId());
    }
}
