package com.activehub.usecases.finalizarclasesvencidas;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.payments.OperacionesPagoMercadoPago;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalizarClasesVencidasService {

    private static final Logger log = LoggerFactory.getLogger(FinalizarClasesVencidasService.class);

    private static final List<EstadoClase> ESTADOS_A_FINALIZAR = List.of(EstadoClase.Programada, EstadoClase.Habilitada);

    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;
    private final OperacionesPagoMercadoPago operacionesPago;
    private final AuditService auditService;
    private final Clock clock;

    public FinalizarClasesVencidasService(
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            PagoRepository pagoRepository,
            OperacionesPagoMercadoPago operacionesPago,
            AuditService auditService,
            Clock clock
    ) {
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.operacionesPago = operacionesPago;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public void finalizar() {
        List<Clase> vencidas = claseRepository.findByEstadoInAndFechaHoraBefore(ESTADOS_A_FINALIZAR, clock.instant());

        for (Clase clase : vencidas) {
            clase.setEstado(EstadoClase.Finalizada);
            claseRepository.save(clase);

            for (Inscripcion inscripcion : inscripcionRepository.findByClaseIdAndEstadoNot(clase.getId(), EstadoInscripcion.CANCELADA)) {
                if (inscripcion.getEstado() == EstadoInscripcion.PRE_INSCRIPCION
                        || inscripcion.getEstado() == EstadoInscripcion.PAGO_PENDIENTE) {
                    inscripcion.setEstado(EstadoInscripcion.CANCELADA);
                    inscripcionRepository.save(inscripcion);
                }
            }

            auditService.registrar(null, AuditAccion.CLASE_FINALIZADA, "Clase", clase.getId(), null);
        }
    }

    /**
     * KAN-65: captura en Mercado Pago los pagos autorizados de las clases ya Finalizadas. Va
     * despues de {@link #finalizar()} y no lo reemplaza: no mira solo las recien finalizadas sino
     * todas las que quedaron sin capturar, asi una captura que fallo se reintenta en la proxima
     * corrida (cada 5 minutos) hasta que entra o vence la autorizacion.
     *
     * <p>Sin {@code @Transactional} a proposito: cada pago commitea solo. Dentro de una
     * transaccion comun, una falla de MP en un pago (o de la lectura del token del instructor,
     * que marca la transaccion para rollback aunque se la atrape) revertiria las capturas de los
     * demas, que en MP ya se hicieron.
     *
     * @return cuantos pagos se intentaron sin error
     */
    public int capturarPagosPendientes() {
        int capturados = 0;
        for (Pago pago : pagoRepository.findSinCapturarDeClasesEn(EstadoPago.Retenido, EstadoClase.Finalizada)) {
            var instructorId = pago.getInscripcion().getClase().getActividad().getInstructor().getId();
            try {
                operacionesPago.capturar(pago, instructorId);
                capturados++;
            } catch (RuntimeException e) {
                log.error("No se pudo capturar el pago {} (orden {}): se reintenta en la proxima corrida",
                        pago.getId(), pago.getMpOrderId(), e);
            }
        }
        return capturados;
    }
}
