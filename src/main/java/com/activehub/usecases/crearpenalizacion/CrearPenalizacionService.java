package com.activehub.usecases.crearpenalizacion;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.domain.penalizacion.VentanaPenalizacion;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.payments.PaymentGateway;
import com.activehub.shared.security.PermisosService;
import com.activehub.shared.time.Zonas;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
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
 * propios datos en el listado.
 *
 * <p><b>Que hace una Suspension temporal</b> (decisiones del usuario, este tramo):
 * <ul>
 *   <li><b>No toca {@code EstadoUsuario}.</b> El penalizado <i>si</i> inicia sesion: tiene que
 *       poder ver su sancion, sus clases canceladas y sus datos. Lo que no puede es operar, y
 *       eso lo hace valer {@code PenalizacionVigenteGuard} mirando la vigencia. Antes esto
 *       ponia la cuenta en SUSPENDIDO, que es la condicion con la que el login rechaza.</li>
 *   <li><b>Cancela las clases del instructor que caen dentro de la vigencia</b>, con la misma
 *       cascada que {@code cancelarclase}: se cancelan las inscripciones, se reintegran los
 *       pagos Retenido y Efectivo, y se le avisa a cada alumno. Un instructor suspendido que
 *       no puede dictar no puede dejar a sus alumnos esperando una clase que no va a existir.</li>
 *   <li><b>Es irreversible.</b> No hay endpoint para borrar ni acortar una penalizacion, a
 *       proposito: es un acto sancionatorio con constancia en auditoria. La pantalla lo avisa
 *       antes de confirmar.</li>
 * </ul>
 *
 * <p>La cascada de cancelacion va inline y no llamando a {@code CancelarClaseService}: es la
 * convencion del repo (no se inyecta el Service de un usecase dentro de otro), la misma que
 * siguen {@code cancelarclase}, {@code notificarausenciaprofesor} y {@code eliminaractividad}.
 */
@Service
public class CrearPenalizacionService {

    private final UsuarioRepository usuarioRepository;
    private final PenalizacionRepository penalizacionRepository;
    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;
    private final NotificacionService notificacionService;
    private final PermisosService permisosService;

    public CrearPenalizacionService(
            UsuarioRepository usuarioRepository,
            PenalizacionRepository penalizacionRepository,
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            PagoRepository pagoRepository,
            PaymentGateway paymentGateway,
            AuditService auditService,
            NotificacionService notificacionService,
            PermisosService permisosService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.penalizacionRepository = penalizacionRepository;
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.paymentGateway = paymentGateway;
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
        int clasesCanceladas = 0;

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
            // NO se toca usuario.estado: el penalizado sigue pudiendo iniciar sesion (ver el
            // javadoc de la clase). Lo que no puede es operar, y de eso se encarga
            // PenalizacionVigenteGuard.
            clasesCanceladas = cancelarClasesDelPeriodo(usuario, request.fechaInicio(), request.fechaFin(), actorId);
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
        String aviso = "Recibiste una penalización (" + etiquetas(aplicadas) + "). Motivo: " + motivo;
        if (clasesCanceladas > 0) {
            aviso += " Se cancelaron " + clasesCanceladas
                    + (clasesCanceladas == 1 ? " clase tuya" : " clases tuyas")
                    + " dentro del período de la suspensión y se reintegró a los inscriptos.";
        }
        notificacionService.notificar(
                // Sin destino: el usuario penalizado no tiene una pantalla donde ver sus
                // penalizaciones (el ABM es del admin), asi que el aviso se muestra sin link.
                usuario.getId(), TipoNotificacion.PENALIZACION_APLICADA, aviso, aplicadas.get(0).getId(), Destino.ninguno());

        return new CrearPenalizacionResponse(
                usuario.getId(), usuario.getCantidadPenalizaciones(), respuesta);
    }

    /**
     * Cancela las clases del instructor que se dictan dentro de la vigencia de la suspension y
     * reintegra a los inscriptos, igual que {@code cancelarclase}.
     *
     * <p>La ventana se arma en zona horaria del negocio ({@link Zonas#AR}): la vigencia se
     * carga como dos fechas de calendario y el ultimo dia cuenta entero, asi que el limite
     * superior es el arranque del dia siguiente a {@code fechaFin}.
     *
     * @return cuantas clases se cancelaron.
     */
    private int cancelarClasesDelPeriodo(Usuario instructor, LocalDate desde, LocalDate hasta, UUID actorId) {
        Instant inicio = desde.atStartOfDay(Zonas.AR).toInstant();
        Instant fin = hasta.plusDays(1).atStartOfDay(Zonas.AR).toInstant();

        List<Clase> clases = claseRepository.findVivasDeInstructorEntre(
                instructor.getId(), inicio, fin, EnumSet.of(EstadoClase.Cancelada, EstadoClase.Finalizada));

        for (Clase clase : clases) {
            String mensaje = "Se canceló la clase de \"" + clase.getActividad().getNombre() + "\" del "
                    + NotificacionMensajes.formatFechaHora(clase.getFechaHora())
                    + " porque el instructor fue suspendido. Tu inscripción fue cancelada.";

            for (Inscripcion inscripcion
                    : inscripcionRepository.findByClaseIdAndEstadoNot(clase.getId(), EstadoInscripcion.CANCELADA)) {
                Pago pago = inscripcion.getPago();
                String detallePago = "";
                if (pago != null && pago.getEstado() == EstadoPago.Retenido) {
                    paymentGateway.cancelarPago(pago.getReferenciaExterna());
                    pago.setEstado(EstadoPago.Cancelado);
                    pagoRepository.save(pago);
                    detallePago = " Se reintegra el pago retenido.";
                } else if (pago != null && pago.getEstado() == EstadoPago.Efectivo) {
                    pago.setEstado(EstadoPago.Cancelado);
                    pagoRepository.save(pago);
                    detallePago = " Coordiná con la plataforma la devolución de lo que pagaste en efectivo.";
                }
                inscripcion.setEstado(EstadoInscripcion.CANCELADA);
                inscripcionRepository.save(inscripcion);

                notificacionService.notificar(
                        inscripcion.getAlumno().getId(), TipoNotificacion.CLASE_CANCELADA,
                        mensaje + detallePago, clase.getId(), Destino.clase(clase.getId()));
            }

            clase.setEstado(EstadoClase.Cancelada);
            claseRepository.save(clase);
            auditService.registrar(
                    actorId, AuditAccion.CLASE_CANCELADA, "Clase", clase.getId(), "PENALIZACION");
        }
        return clases.size();
    }

    private String etiquetas(List<Penalizacion> penalizaciones) {
        return penalizaciones.stream().map(p -> p.getTipo().getEtiqueta()).reduce((a, b) -> a + " + " + b).orElse("");
    }
}
