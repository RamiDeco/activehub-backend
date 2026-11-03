package com.activehub.usecases.enviarcomprobantepenalizacion;

import com.activehub.domain.penalizacion.ComprobantePenalizacion;
import com.activehub.domain.penalizacion.ComprobantePenalizacionRepository;
import com.activehub.domain.penalizacion.EstadoComprobantePenalizacion;
import com.activehub.domain.penalizacion.EstadoPagoPenalizacion;
import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.permiso.ConfiguracionRolRepository;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.storage.AlmacenamientoArchivos;
import com.activehub.shared.storage.AlmacenamientoException;
import com.activehub.shared.storage.CarpetaArchivos;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * El instructor manda el comprobante de la transferencia con la que pago su multa (V36).
 *
 * <p>La multa pasa a EN_REVISION y se avisa a quienes revisan penalizaciones. Mientras espera
 * sigue bloqueado: lo desbloquea el administrador al aprobar el comprobante. Si lo marca
 * invalido, la multa vuelve a PENDIENTE_PAGO y puede mandar otro.
 */
@Service
public class EnviarComprobantePenalizacionService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("application/pdf", "image/jpeg", "image/png");
    /** Quien recibe el aviso: el mismo permiso que protege la pantalla de Penalizaciones. */
    static final String PERMISO_REVISION = "penalizaciones.gestionar";

    private final PenalizacionRepository penalizacionRepository;
    private final ComprobantePenalizacionRepository comprobantePenalizacionRepository;
    private final ConfiguracionRolRepository configuracionRolRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final AuditService auditService;
    private final AlmacenamientoArchivos almacenamiento;

    public EnviarComprobantePenalizacionService(
            PenalizacionRepository penalizacionRepository,
            ComprobantePenalizacionRepository comprobantePenalizacionRepository,
            ConfiguracionRolRepository configuracionRolRepository,
            UsuarioRepository usuarioRepository,
            NotificacionService notificacionService,
            AuditService auditService,
            AlmacenamientoArchivos almacenamiento) {
        this.penalizacionRepository = penalizacionRepository;
        this.comprobantePenalizacionRepository = comprobantePenalizacionRepository;
        this.configuracionRolRepository = configuracionRolRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
        this.auditService = auditService;
        this.almacenamiento = almacenamiento;
    }

    @Transactional
    public EnviarComprobantePenalizacionResponse enviar(UUID penalizacionId, MultipartFile archivo, UUID actorId) {
        Penalizacion multa = penalizacionRepository.findById(penalizacionId)
                // Una multa ajena es un 404 y no un 403: no se confirma que exista.
                .filter(p -> p.getUsuario().getId().equals(actorId))
                .orElseThrow(() -> new NoEncontradoException("Penalización no encontrada."));

        if (multa.getEstadoPago() == null) {
            throw new ValidacionException("Esta penalización no se paga desde la plataforma.");
        }
        switch (multa.getEstadoPago()) {
            case EN_REVISION -> throw new ValidacionException(
                    "Ya enviaste un comprobante: esperá a que la administración lo revise.");
            case PAGADA -> throw new ValidacionException("Esta multa ya está pagada.");
            case PENDIENTE_PAGO -> {
                // se puede enviar
            }
        }

        if (archivo == null || archivo.isEmpty()) {
            throw new ValidacionException("Seleccioná el comprobante de pago.");
        }
        String contentType = archivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new ValidacionException("Formato no permitido. Subí un PDF, JPG o PNG.");
        }

        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "comprobante";
        String nombreGuardado = UUID.randomUUID() + extension(nombreOriginal);
        try {
            almacenamiento.guardar(
                    CarpetaArchivos.COMPROBANTES_PENALIZACION, nombreGuardado, archivo.getBytes(), contentType);
        } catch (IOException | AlmacenamientoException e) {
            throw new ValidacionException("No pudimos guardar el comprobante. Intentá de nuevo.");
        }

        ComprobantePenalizacion comprobante = new ComprobantePenalizacion();
        comprobante.setPenalizacion(multa);
        comprobante.setRutaArchivo(nombreGuardado);
        comprobante.setNombreArchivo(nombreOriginal);
        comprobante.setTipoContenido(contentType);
        comprobante.setTamanioBytes(archivo.getSize());
        comprobante.setEstado(EstadoComprobantePenalizacion.EN_REVISION);
        comprobante = comprobantePenalizacionRepository.save(comprobante);

        multa.setEstadoPago(EstadoPagoPenalizacion.EN_REVISION);
        penalizacionRepository.save(multa);

        auditService.registrar(
                actorId, AuditAccion.COMPROBANTE_PENALIZACION_ENVIADO, "Penalizacion", multa.getId(), null);

        avisarAdministracion(multa);

        return new EnviarComprobantePenalizacionResponse(
                multa.getId(), comprobante.getId(), multa.getEstadoPago().name());
    }

    private void avisarAdministracion(Penalizacion multa) {
        List<UUID> roles = configuracionRolRepository.rolesConPermiso(PERMISO_REVISION);
        // Con la lista vacia el IN de la consulta no es valido en Postgres.
        if (roles.isEmpty()) {
            return;
        }
        String mensaje = multa.getUsuario().getNombre() + " " + multa.getUsuario().getApellido()
                + " envió el comprobante de pago de su multa. Revisalo para desbloquearlo.";
        for (UUID adminId : usuarioRepository.findIdsActivosPorRol(roles)) {
            notificacionService.notificar(adminId, TipoNotificacion.COMPROBANTE_PENALIZACION_ENVIADO, mensaje,
                    multa.getId(), Destino.penalizacion(multa.getId()));
        }
    }

    private String extension(String nombreOriginal) {
        int i = nombreOriginal.lastIndexOf('.');
        return i >= 0 ? nombreOriginal.substring(i) : "";
    }
}
