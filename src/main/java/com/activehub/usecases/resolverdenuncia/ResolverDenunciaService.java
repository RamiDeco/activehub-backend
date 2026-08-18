package com.activehub.usecases.resolverdenuncia;

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

    public ResolverDenunciaService(
            DenunciaRepository denunciaRepository,
            InscripcionRepository inscripcionRepository,
            PagoRepository pagoRepository,
            UsuarioRepository usuarioRepository,
            PenalizacionRepository penalizacionRepository,
            PaymentGateway paymentGateway,
            AuditService auditService
    ) {
        this.denunciaRepository = denunciaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.penalizacionRepository = penalizacionRepository;
        this.paymentGateway = paymentGateway;
        this.auditService = auditService;
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
            case SUSPENDER -> suspenderInstructor(denuncia);
            case PENALIZAR -> penalizarInstructor(denuncia);
            case DESESTIMAR -> {
                // sin efecto secundario: se desestima y se cierra el caso.
            }
        }

        denuncia.setEstado(EstadoDenuncia.RESUELTA);

        auditService.registrar(actorId, AuditAccion.DENUNCIA_RESUELTA, "Denuncia", id, accion.name());

        return new ResolverDenunciaResponse(denuncia.getId(), denuncia.getEstado().getEtiqueta());
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

    private void suspenderInstructor(Denuncia denuncia) {
        Usuario instructor = denuncia.getClase().getActividad().getInstructor();
        instructor.setEstado(EstadoUsuario.SUSPENDIDO);
        usuarioRepository.save(instructor);
    }

    private void penalizarInstructor(Denuncia denuncia) {
        Usuario instructor = denuncia.getClase().getActividad().getInstructor();

        Penalizacion penalizacion = new Penalizacion();
        penalizacion.setUsuario(instructor);
        penalizacion.setTipo(TipoPenalizacion.ECONOMICA);
        penalizacion.setMotivo(denuncia.getMotivo());
        penalizacionRepository.save(penalizacion);

        instructor.setCantidadPenalizaciones(instructor.getCantidadPenalizaciones() + 1);
        usuarioRepository.save(instructor);
    }
}
