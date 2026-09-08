package com.activehub.usecases.crearpenalizacion;

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
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E4Ad-HU06 criterio 2: el admin aplica una penalizacion a mano desde la pantalla de
 * Penalizaciones. Hasta ahora las Penalizacion solo nacian dentro de {@code resolverdenuncia},
 * asi que el boton "Nueva penalizacion" no tenia backend y solo mutaba un array del navegador.
 *
 * <p>Aplicar una Suspension temporal deja al usuario en SUSPENDIDO; la vigencia la levanta
 * despues {@code levantarsuspensionesvencidas}.
 */
@Service
public class CrearPenalizacionService {

    private final UsuarioRepository usuarioRepository;
    private final PenalizacionRepository penalizacionRepository;
    private final AuditService auditService;
    private final NotificacionService notificacionService;

    public CrearPenalizacionService(
            UsuarioRepository usuarioRepository,
            PenalizacionRepository penalizacionRepository,
            AuditService auditService,
            NotificacionService notificacionService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.penalizacionRepository = penalizacionRepository;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public CrearPenalizacionResponse crear(CrearPenalizacionRequest request, UUID actorId) {
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        if (usuario.getId().equals(actorId)) {
            throw new ValidacionException("No podés penalizarte a vos mismo.");
        }

        TipoPenalizacion tipo = TipoPenalizacion.fromEtiqueta(request.tipo());

        Penalizacion penalizacion = new Penalizacion();
        penalizacion.setUsuario(usuario);
        penalizacion.setTipo(tipo);
        penalizacion.setMotivo(request.motivo().trim());

        if (tipo == TipoPenalizacion.ECONOMICA) {
            if (request.monto() == null || request.monto().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidacionException("Ingresá el monto de la penalización económica.");
            }
            penalizacion.setMonto(request.monto());
        } else {
            if (request.fechaInicio() == null || request.fechaFin() == null) {
                throw new ValidacionException("Indicá la fecha de inicio y de fin de la suspensión.");
            }
            if (request.fechaFin().isBefore(request.fechaInicio())) {
                throw new ValidacionException("La fecha de fin debe ser posterior a la de inicio.");
            }
            penalizacion.setFechaInicio(request.fechaInicio());
            penalizacion.setFechaFin(request.fechaFin());
            usuario.setEstado(EstadoUsuario.SUSPENDIDO);
        }

        usuario.setCantidadPenalizaciones(usuario.getCantidadPenalizaciones() + 1);
        usuarioRepository.save(usuario);
        penalizacion = penalizacionRepository.saveAndFlush(penalizacion);

        notificacionService.notificar(
                usuario.getId(),
                TipoNotificacion.PENALIZACION_APLICADA,
                "Recibiste una penalización (" + tipo.getEtiqueta() + "). Motivo: " + penalizacion.getMotivo(),
                penalizacion.getId());

        auditService.registrar(
                actorId, AuditAccion.PENALIZACION_APLICADA, "Penalizacion", penalizacion.getId(), tipo.name());

        return new CrearPenalizacionResponse(
                penalizacion.getId(), usuario.getId(), tipo.getEtiqueta(),
                penalizacion.getMotivo(), penalizacion.getMonto(),
                penalizacion.getFechaInicio(), penalizacion.getFechaFin(),
                usuario.getCantidadPenalizaciones());
    }
}
