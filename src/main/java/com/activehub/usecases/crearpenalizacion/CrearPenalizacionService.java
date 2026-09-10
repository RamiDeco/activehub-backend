package com.activehub.usecases.crearpenalizacion;

import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.domain.penalizacion.VentanaPenalizacion;
import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.security.PermisosService;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E4Ad-HU06 criterio 2: el admin aplica una penalizacion a mano desde la pantalla de
 * Penalizaciones. Hasta ahora las Penalizacion solo nacian dentro de {@code resolverdenuncia},
 * asi que el boton "Nueva penalizacion" no tenia backend y solo mutaba un array del navegador.
 *
 * <p><b>Se pueden aplicar los dos tipos de una sola vez</b> (multa + suspension). Se guarda una
 * fila por tipo: el enum de la base sigue teniendo dos valores y cada sancion queda con sus
 * propios datos en el listado. Aplicar una Suspension temporal deja al usuario en SUSPENDIDO;
 * la vigencia la levanta despues {@code levantarsuspensionesvencidas}.
 */
@Service
public class CrearPenalizacionService {

    private final UsuarioRepository usuarioRepository;
    private final PenalizacionRepository penalizacionRepository;
    private final AuditService auditService;
    private final NotificacionService notificacionService;
    private final PermisosService permisosService;

    public CrearPenalizacionService(
            UsuarioRepository usuarioRepository,
            PenalizacionRepository penalizacionRepository,
            AuditService auditService,
            NotificacionService notificacionService,
            PermisosService permisosService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.penalizacionRepository = penalizacionRepository;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
        this.permisosService = permisosService;
    }

    @Transactional
    public CrearPenalizacionResponse crear(CrearPenalizacionRequest request, UUID actorId) {
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        if (usuario.getId().equals(actorId)) {
            throw new ValidacionException("No podés penalizarte a vos mismo.");
        }

        // La penalizacion existe por la inasistencia del profesor (E4Ad-HU06 / RN-13): la multa
        // y la suspension solo tienen sentido sobre quien dicta clases. Penalizar a un alumno o
        // a un administrador no significa nada, y hasta aca el formulario los ofrecia igual.
        // Se pregunta por el permiso, no por el nombre del rol (RN-19): un rol nuevo con
        // `clases.gestionar` tambien es penalizable.
        if (!permisosService.puede(usuario.getId(), "clases.gestionar")) {
            throw new ValidacionException(
                    "Solo se puede penalizar a un instructor: las sanciones existen por la inasistencia del profesor.");
        }

        // Un mismo tipo repetido en la lista es un error del cliente, no dos sanciones.
        Set<TipoPenalizacion> tipos = new LinkedHashSet<>();
        for (String etiqueta : request.tipos()) {
            tipos.add(TipoPenalizacion.fromEtiqueta(etiqueta));
        }

        String motivo = request.motivo().trim();
        List<Penalizacion> aplicadas = new ArrayList<>();

        if (tipos.contains(TipoPenalizacion.ECONOMICA)) {
            if (request.monto() == null || request.monto().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidacionException("Ingresá el monto de la penalización económica.");
            }
            Penalizacion multa = new Penalizacion();
            multa.setUsuario(usuario);
            multa.setTipo(TipoPenalizacion.ECONOMICA);
            multa.setMotivo(motivo);
            multa.setMonto(request.monto());
            aplicadas.add(multa);
        }

        if (tipos.contains(TipoPenalizacion.SUSPENSION_TEMPORAL)) {
            if (request.fechaInicio() == null || request.fechaFin() == null) {
                throw new ValidacionException("Indicá la fecha de inicio y de fin de la suspensión.");
            }
            if (request.fechaFin().isBefore(request.fechaInicio())) {
                throw new ValidacionException("La fecha de fin debe ser posterior a la de inicio.");
            }
            long dias = ChronoUnit.DAYS.between(request.fechaInicio(), request.fechaFin());
            if (dias < VentanaPenalizacion.MINIMO_DIAS_SUSPENSION) {
                throw new ValidacionException(
                        "La suspensión no puede durar menos de " + VentanaPenalizacion.MINIMO_DIAS_SUSPENSION + " días.");
            }
            Penalizacion suspension = new Penalizacion();
            suspension.setUsuario(usuario);
            suspension.setTipo(TipoPenalizacion.SUSPENSION_TEMPORAL);
            suspension.setMotivo(motivo);
            suspension.setFechaInicio(request.fechaInicio());
            suspension.setFechaFin(request.fechaFin());
            aplicadas.add(suspension);
            usuario.setEstado(EstadoUsuario.SUSPENDIDO);
        }

        usuario.setCantidadPenalizaciones(usuario.getCantidadPenalizaciones() + aplicadas.size());
        usuarioRepository.save(usuario);

        List<CrearPenalizacionResponse.Aplicada> respuesta = new ArrayList<>();
        for (Penalizacion penalizacion : aplicadas) {
            Penalizacion guardada = penalizacionRepository.saveAndFlush(penalizacion);
            auditService.registrar(
                    actorId, AuditAccion.PENALIZACION_APLICADA, "Penalizacion", guardada.getId(),
                    guardada.getTipo().name());
            respuesta.add(new CrearPenalizacionResponse.Aplicada(
                    guardada.getId(), guardada.getTipo().getEtiqueta(), guardada.getMotivo(),
                    guardada.getMonto(), guardada.getFechaInicio(), guardada.getFechaFin()));
        }

        // Una sola notificación aunque sean dos sanciones: para el usuario es un solo hecho.
        notificacionService.notificar(
                usuario.getId(),
                TipoNotificacion.PENALIZACION_APLICADA,
                "Recibiste una penalización (" + etiquetas(aplicadas) + "). Motivo: " + motivo,
                aplicadas.get(0).getId());

        return new CrearPenalizacionResponse(
                usuario.getId(), usuario.getCantidadPenalizaciones(), respuesta);
    }

    private String etiquetas(List<Penalizacion> penalizaciones) {
        return penalizaciones.stream().map(p -> p.getTipo().getEtiqueta()).reduce((a, b) -> a + " + " + b).orElse("");
    }
}
