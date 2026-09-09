package com.activehub.usecases.eliminaractividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
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
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.payments.PaymentGateway;
import java.util.EnumSet;
import java.util.List;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarActividadService {

    private final ActividadRepository actividadRepository;
    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;
    private final NotificacionService notificacionService;

    private final InstructorVerificadoGuard instructorVerificadoGuard;

    public EliminarActividadService(
            ActividadRepository actividadRepository,
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            PagoRepository pagoRepository,
            PaymentGateway paymentGateway,
            AuditService auditService,
            NotificacionService notificacionService,
            InstructorVerificadoGuard instructorVerificadoGuard
    ) {
        this.actividadRepository = actividadRepository;
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.paymentGateway = paymentGateway;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
        this.instructorVerificadoGuard = instructorVerificadoGuard;
    }

    @Transactional
    public void eliminar(UUID id, UUID actorId, boolean esAdmin) {
        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));

        if (!esAdmin && !actividad.getInstructor().getId().equals(actorId)) {
            throw new SinPermisoException("No podés eliminar una actividad que no te pertenece.");
        }
        // El admin no necesita perfil de instructor verificado: la guarda solo aplica
        // cuando el que borra es el propio instructor.
        if (!esAdmin) {
            instructorVerificadoGuard.exigirVerificado(actorId, "eliminar actividades");
        }

        List<Clase> clasesVigentes = claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(
                id, EnumSet.of(EstadoClase.Cancelada, EstadoClase.Finalizada));

        for (Clase clase : clasesVigentes) {
            String contexto = "la clase de \"" + actividad.getNombre() + "\" del " + NotificacionMensajes.formatFechaHora(clase.getFechaHora());
            for (Inscripcion inscripcion : inscripcionRepository.findByClaseIdAndEstadoNot(clase.getId(), EstadoInscripcion.CANCELADA)) {
                Pago pago = inscripcion.getPago();
                if (pago != null && pago.getEstado() == EstadoPago.Retenido) {
                    paymentGateway.cancelarPago(pago.getReferenciaExterna());
                    pago.setEstado(EstadoPago.Cancelado);
                    pagoRepository.save(pago);
                }
                inscripcion.setEstado(EstadoInscripcion.CANCELADA);
                inscripcionRepository.save(inscripcion);

                notificacionService.notificar(
                        inscripcion.getAlumno().getId(),
                        TipoNotificacion.ACTIVIDAD_ELIMINADA,
                        "Se eliminó la actividad \"" + actividad.getNombre() + "\" y tu inscripción a " + contexto + " fue cancelada.",
                        actividad.getId());
            }

            clase.setEstado(EstadoClase.Cancelada);
            claseRepository.save(clase);

            auditService.registrar(actorId, AuditAccion.CLASE_CANCELADA, "Clase", clase.getId(), null);
        }

        boolean loBorraUnAdmin = esAdmin && !actividad.getInstructor().getId().equals(actorId);
        if (loBorraUnAdmin) {
            notificacionService.notificar(
                    actividad.getInstructor().getId(),
                    TipoNotificacion.ACTIVIDAD_ELIMINADA,
                    "Un administrador eliminó tu actividad \"" + actividad.getNombre() + "\" y se cancelaron sus clases e inscripciones.",
                    actividad.getId());
        }

        actividad.marcarBorrado();
        actividadRepository.save(actividad);

        auditService.registrar(actorId, AuditAccion.ACTIVIDAD_ELIMINADA, "Actividad", id, null);
    }
}
