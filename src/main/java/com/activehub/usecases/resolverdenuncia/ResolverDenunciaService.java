package com.activehub.usecases.resolverdenuncia;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.denuncia.EstadoDenuncia;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.payments.PaymentGateway;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResolverDenunciaService {

    private final DenunciaRepository denunciaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PenalizacionRepository penalizacionRepository;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;
    private final NotificacionService notificacionService;

    public ResolverDenunciaService(
            DenunciaRepository denunciaRepository,
            InscripcionRepository inscripcionRepository,
            PagoRepository pagoRepository,
            UsuarioRepository usuarioRepository,
            PenalizacionRepository penalizacionRepository,
            PaymentGateway paymentGateway,
            AuditService auditService,
            NotificacionService notificacionService
    ) {
        this.denunciaRepository = denunciaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.penalizacionRepository = penalizacionRepository;
        this.paymentGateway = paymentGateway;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public ResolverDenunciaResponse resolver(UUID id, ResolverDenunciaRequest request, UUID actorId) {
        Denuncia denuncia = denunciaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Denuncia no encontrada."));

        if (denuncia.getEstado() == EstadoDenuncia.RESUELTA) {
            throw new ValidacionException("Esta denuncia ya fue resuelta.");
        }

        AccionResolucion accion;
        try {
            accion = AccionResolucion.valueOf(request.accion());
        } catch (IllegalArgumentException ex) {
            throw new ValidacionException("Acción de resolución inválida: " + request.accion());
        }

        switch (accion) {
            case REINTEGRAR -> reintegrar(denuncia);
            case SUSPENDER -> suspenderInstructor(denuncia, actorId);
            case PENALIZAR -> penalizarInstructor(denuncia, actorId);
            case DESESTIMAR -> {
                // sin efecto secundario: se desestima y se cierra el caso.
            }
        }

        denuncia.setEstado(EstadoDenuncia.RESUELTA);

        notificarResolucion(denuncia, accion);

        auditService.registrar(actorId, AuditAccion.DENUNCIA_RESUELTA, "Denuncia", id, accion.name());

        return new ResolverDenunciaResponse(denuncia.getId(), denuncia.getEstado().getEtiqueta());
    }

    private void notificarResolucion(Denuncia denuncia, AccionResolucion accion) {
        Clase clase = denuncia.getClase();
        Actividad actividad = clase.getActividad();
        String contexto = "la clase de \"" + actividad.getNombre() + "\" del " + NotificacionMensajes.formatFechaHora(clase.getFechaHora());

        String mensajeAlumno = switch (accion) {
            case REINTEGRAR -> "Se resolvió tu denuncia sobre " + contexto + ": te reintegramos el pago.";
            case SUSPENDER -> "Se resolvió tu denuncia sobre " + contexto + ": el instructor fue suspendido.";
            case PENALIZAR -> "Se resolvió tu denuncia sobre " + contexto + ": se le aplicó una penalización al instructor.";
            case DESESTIMAR -> "Se desestimó tu denuncia sobre " + contexto + ".";
        };
        notificacionService.notificar(denuncia.getAlumno().getId(), TipoNotificacion.DENUNCIA_RESUELTA, mensajeAlumno, denuncia.getId());

        if (accion == AccionResolucion.SUSPENDER || accion == AccionResolucion.PENALIZAR || accion == AccionResolucion.DESESTIMAR) {
            Usuario instructor = actividad.getInstructor();
            String mensajeInstructor = accion == AccionResolucion.SUSPENDER
                    ? "Fuiste suspendido por una denuncia sobre " + contexto + "."
                    : accion == AccionResolucion.PENALIZAR
                            ? "Se te aplicó una penalización económica por una denuncia sobre " + contexto + "."
                            : "Una denuncia en tu contra sobre " + contexto + " fue desestimada.";
            TipoNotificacion tipo = accion == AccionResolucion.SUSPENDER ? TipoNotificacion.INSTRUCTOR_SUSPENDIDO
                    : accion == AccionResolucion.PENALIZAR ? TipoNotificacion.PENALIZACION_APLICADA
                    : TipoNotificacion.DENUNCIA_DESESTIMADA;
            notificacionService.notificar(instructor.getId(), tipo, mensajeInstructor, denuncia.getId());
        }
    }

    private void reintegrar(Denuncia denuncia) {
        inscripcionRepository
                .findByClaseIdAndAlumnoIdAndEstadoNot(
                        denuncia.getClase().getId(), denuncia.getAlumno().getId(), EstadoInscripcion.CANCELADA)
                .ifPresent(inscripcion -> {
                    Pago pago = inscripcion.getPago();
                    if (pago != null && pago.getEstado() == EstadoPago.Retenido) {
                        paymentGateway.cancelarPago(pago.getReferenciaExterna());
                        pago.setEstado(EstadoPago.Cancelado);
                        pagoRepository.save(pago);
                    }
                    inscripcion.setEstado(EstadoInscripcion.CANCELADA);
                    inscripcionRepository.save(inscripcion);
                });
    }

    private void suspenderInstructor(Denuncia denuncia, UUID actorId) {
        Usuario instructor = denuncia.getClase().getActividad().getInstructor();
        instructor.setEstado(EstadoUsuario.SUSPENDIDO);
        usuarioRepository.save(instructor);
        // RN-14: la suspensión es una operación crítica y necesita su propio registro. Antes
        // solo quedaba la fila DENUNCIA_RESUELTA, sin rastro sobre el usuario sancionado.
        auditService.registrar(
                actorId, AuditAccion.USUARIO_ESTADO_ACTUALIZADO, "Usuario", instructor.getId(), "SUSPENDIDO");
    }

    private void penalizarInstructor(Denuncia denuncia, UUID actorId) {
        Usuario instructor = denuncia.getClase().getActividad().getInstructor();

        Penalizacion penalizacion = new Penalizacion();
        penalizacion.setUsuario(instructor);
        penalizacion.setTipo(TipoPenalizacion.ECONOMICA);
        penalizacion.setMotivo(denuncia.getMotivo());
        // Vínculo con su origen: habilita el "Ver denuncia" del listado (criterio 6).
        penalizacion.setDenuncia(denuncia);
        penalizacion = penalizacionRepository.saveAndFlush(penalizacion);

        instructor.setCantidadPenalizaciones(instructor.getCantidadPenalizaciones() + 1);
        usuarioRepository.save(instructor);

        auditService.registrar(
                actorId, AuditAccion.PENALIZACION_APLICADA, "Penalizacion", penalizacion.getId(), "ECONOMICA");
    }
}
