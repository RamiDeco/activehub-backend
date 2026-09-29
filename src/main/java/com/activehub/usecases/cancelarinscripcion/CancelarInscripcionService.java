package com.activehub.usecases.cancelarinscripcion;

import com.activehub.domain.actividad.ClaseRepository;
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
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelarInscripcionService {

    private final InscripcionRepository inscripcionRepository;
    private final ClaseRepository claseRepository;
    private final PagoRepository pagoRepository;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;
    private final NotificacionService notificacionService;
    private final Clock clock;

    public CancelarInscripcionService(
            InscripcionRepository inscripcionRepository,
            ClaseRepository claseRepository,
            PagoRepository pagoRepository,
            PaymentGateway paymentGateway,
            AuditService auditService,
            NotificacionService notificacionService,
            Clock clock
    ) {
        this.inscripcionRepository = inscripcionRepository;
        this.claseRepository = claseRepository;
        this.pagoRepository = pagoRepository;
        this.paymentGateway = paymentGateway;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
        this.clock = clock;
    }

    @Transactional
    public void cancelar(UUID id, UUID alumnoId) {
        Inscripcion inscripcion = inscripcionRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Inscripción no encontrada."));

        if (!inscripcion.getAlumno().getId().equals(alumnoId)) {
            throw new SinPermisoException("No podés cancelar una inscripción que no te pertenece.");
        }

        if (inscripcion.getEstado() == EstadoInscripcion.CANCELADA) {
            throw new ValidacionException("Esta inscripción ya está cancelada.");
        }

        if (!clock.instant().isBefore(inscripcion.getClase().getFechaHora())) {
            throw new ValidacionException("No podés cancelar una inscripción de una clase que ya pasó.");
        }

        // Leer Pago y los datos de la actividad ANTES del UPDATE atomico: liberarCupo usa
        // clearAutomatically=true, que limpia todo el persistence context y dejaria estos
        // proxies lazy huerfanos si se acceden despues.
        Pago pago = inscripcion.getPago();
        var actividadNombre = inscripcion.getClase().getActividad().getNombre();
        var instructorId = inscripcion.getClase().getActividad().getInstructor().getId();
        var fechaHoraClase = inscripcion.getClase().getFechaHora();
        var claseId = inscripcion.getClase().getId();

        if (inscripcion.getEstado() == EstadoInscripcion.INSCRIPTO || inscripcion.getEstado() == EstadoInscripcion.PAGO_PENDIENTE) {
            claseRepository.liberarCupo(inscripcion.getClase().getId());
        }

        if (pago != null && pago.getEstado() == EstadoPago.Retenido) {
            paymentGateway.cancelarPago(pago.getReferenciaExterna());
            pago.setEstado(EstadoPago.Cancelado);
            pagoRepository.save(pago);
        }

        inscripcion.setEstado(EstadoInscripcion.CANCELADA);
        inscripcionRepository.save(inscripcion);

        String contexto = "la clase de \"" + actividadNombre + "\" del " + NotificacionMensajes.formatFechaHora(fechaHoraClase);
        notificacionService.notificar(
                alumnoId, TipoNotificacion.INSCRIPCION_CANCELADA, "Cancelaste tu inscripción a " + contexto + ".",
                inscripcion.getId(), Destino.inscripcion(inscripcion.getId()));
        notificacionService.notificar(
                instructorId, TipoNotificacion.ALUMNO_CANCELO_INSCRIPCION,
                "Un alumno canceló su inscripción a " + contexto + ".", inscripcion.getId(),
                // El instructor quiere ver el roster que quedo, no la fila que se dio de baja.
                Destino.clase(claseId));

        auditService.registrar(alumnoId, AuditAccion.INSCRIPCION_CANCELADA, "Inscripcion", inscripcion.getId(), null);
    }
}
