package com.activehub.usecases.resolverdenuncia;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.denuncia.EstadoDenuncia;
import com.activehub.domain.denuncia.ResolucionDenuncia;
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
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
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
import com.activehub.shared.time.Zonas;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResolverDenunciaService {

    private final DenunciaRepository denunciaRepository;
    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PenalizacionRepository penalizacionRepository;
    private final ReseniaRepository reseniaRepository;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;
    private final NotificacionService notificacionService;
    private final Clock clock;

    public ResolverDenunciaService(
            DenunciaRepository denunciaRepository,
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            PagoRepository pagoRepository,
            UsuarioRepository usuarioRepository,
            PenalizacionRepository penalizacionRepository,
            ReseniaRepository reseniaRepository,
            PaymentGateway paymentGateway,
            AuditService auditService,
            NotificacionService notificacionService,
            Clock clock
    ) {
        this.denunciaRepository = denunciaRepository;
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.penalizacionRepository = penalizacionRepository;
        this.reseniaRepository = reseniaRepository;
        this.paymentGateway = paymentGateway;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
        this.clock = clock;
    }

    @Transactional
    public ResolverDenunciaResponse resolver(UUID id, ResolverDenunciaRequest request, UUID actorId) {
        Denuncia denuncia = denunciaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Denuncia no encontrada."));

        if (denuncia.getEstado() == EstadoDenuncia.RESUELTA) {
            throw new ValidacionException("Esta denuncia ya fue resuelta.");
        }

        ResolucionDenuncia accion;
        try {
            accion = ResolucionDenuncia.valueOf(request.accion());
        } catch (IllegalArgumentException ex) {
            throw new ValidacionException("Acción de resolución inválida: " + request.accion());
        }

        boolean sobreResenia = denuncia.getResenia() != null;
        // Las acciones que tocan el pago o la clase no aplican a una denuncia de reseña, y
        // OCULTAR_RESENIA no aplica a una de clase: sin esta guarda el switch explotaba con
        // un NullPointerException dentro de la transacción.
        if (sobreResenia && accion != ResolucionDenuncia.OCULTAR_RESENIA
                && accion != ResolucionDenuncia.DESESTIMAR) {
            throw new ValidacionException(
                    "Sobre una reseña denunciada solo podés ocultarla o desestimar la denuncia.");
        }
        if (!sobreResenia && accion == ResolucionDenuncia.OCULTAR_RESENIA) {
            throw new ValidacionException("Esta denuncia no es sobre una reseña.");
        }

        switch (accion) {
            case REINTEGRAR -> reintegrar(denuncia);
            case SUSPENDER -> suspenderInstructor(denuncia, request, actorId);
            case PENALIZAR -> penalizarInstructor(denuncia, actorId);
            case OCULTAR_RESENIA -> ocultarResenia(denuncia, actorId);
            case DESESTIMAR -> {
                // sin efecto secundario: se desestima y se cierra el caso.
            }
        }

        denuncia.setEstado(EstadoDenuncia.RESUELTA);
        // Se persiste el resultado, no solo el estado: sin esto el denunciante veía
        // "Resuelta" sin saber qué se decidió (E3A-HU11 criterios 2 y 7).
        denuncia.setResolucion(accion);
        denuncia.setDetalle(request.detalle() != null && !request.detalle().isBlank()
                ? request.detalle().trim()
                : null);
        denunciaRepository.save(denuncia);

        notificarResolucion(denuncia, accion);

        auditService.registrar(actorId, AuditAccion.DENUNCIA_RESUELTA, "Denuncia", id, accion.name());

        return new ResolverDenunciaResponse(denuncia.getId(), denuncia.getEstado().getEtiqueta(), accion.name());
    }

    private void notificarResolucion(Denuncia denuncia, ResolucionDenuncia accion) {
        if (denuncia.getResenia() != null) {
            notificarResolucionResenia(denuncia, accion);
            return;
        }

        Clase clase = denuncia.getClase();
        Actividad actividad = clase.getActividad();
        String contexto = "la clase de \"" + actividad.getNombre() + "\" del " + NotificacionMensajes.formatFechaHora(clase.getFechaHora());

        String mensajeAlumno = switch (accion) {
            case REINTEGRAR -> "Se resolvió tu denuncia sobre " + contexto + ": te reintegramos el pago.";
            case SUSPENDER -> "Se resolvió tu denuncia sobre " + contexto + ": el instructor fue suspendido.";
            case PENALIZAR -> "Se resolvió tu denuncia sobre " + contexto + ": se le aplicó una penalización al instructor.";
            case DESESTIMAR -> "Se desestimó tu denuncia sobre " + contexto + ".";
            case OCULTAR_RESENIA -> throw new IllegalStateException("OCULTAR_RESENIA no aplica a una denuncia de clase.");
        };
        notificacionService.notificar(denuncia.getAlumno().getId(), TipoNotificacion.DENUNCIA_RESUELTA, mensajeAlumno, denuncia.getId());

        if (accion == ResolucionDenuncia.SUSPENDER || accion == ResolucionDenuncia.PENALIZAR
                || accion == ResolucionDenuncia.DESESTIMAR) {
            Usuario instructor = actividad.getInstructor();
            String mensajeInstructor = accion == ResolucionDenuncia.SUSPENDER
                    ? "Fuiste suspendido por una denuncia sobre " + contexto + "."
                    : accion == ResolucionDenuncia.PENALIZAR
                            ? "Se te aplicó una penalización económica por una denuncia sobre " + contexto + "."
                            : "Una denuncia en tu contra sobre " + contexto + " fue desestimada.";
            TipoNotificacion tipo = accion == ResolucionDenuncia.SUSPENDER ? TipoNotificacion.INSTRUCTOR_SUSPENDIDO
                    : accion == ResolucionDenuncia.PENALIZAR ? TipoNotificacion.PENALIZACION_APLICADA
                    : TipoNotificacion.DENUNCIA_DESESTIMADA;
            notificacionService.notificar(instructor.getId(), tipo, mensajeInstructor, denuncia.getId());
        }
    }

    private void notificarResolucionResenia(Denuncia denuncia, ResolucionDenuncia accion) {
        Resenia resenia = denuncia.getResenia();
        String actividadNombre = resenia.getClase().getActividad().getNombre();

        // Al instructor que denunció.
        String mensajeDenunciante = accion == ResolucionDenuncia.OCULTAR_RESENIA
                ? "Se resolvió tu denuncia sobre una reseña de \"" + actividadNombre + "\": la reseña fue ocultada."
                : "Se desestimó tu denuncia sobre una reseña de \"" + actividadNombre + "\".";
        notificacionService.notificar(
                denuncia.getDenunciante().getId(), TipoNotificacion.DENUNCIA_RESUELTA, mensajeDenunciante, denuncia.getId());

        // Al alumno autor de la reseña, solo si le ocultaron el contenido.
        if (accion == ResolucionDenuncia.OCULTAR_RESENIA) {
            notificacionService.notificar(
                    resenia.getAlumno().getId(),
                    TipoNotificacion.DENUNCIA_RESUELTA,
                    "Tu reseña sobre \"" + actividadNombre + "\" fue ocultada tras una denuncia.",
                    denuncia.getId());
        }
    }

    private void ocultarResenia(Denuncia denuncia, UUID actorId) {
        Resenia resenia = denuncia.getResenia();
        resenia.setOculta(true);
        reseniaRepository.save(resenia);
        auditService.registrar(actorId, AuditAccion.RESENIA_RECHAZADA, "Resenia", resenia.getId(), "OCULTA_POR_DENUNCIA");
    }

    /**
     * Reintegra a <b>TODA la clase</b>, no sólo a quien denunció.
     *
     * <p>Decision del usuario, y corrige el comportamiento anterior: antes esto buscaba una
     * sola inscripcion —la del denunciante— y devolvia ese unico pago. Pero lo que se denuncia
     * es un hecho de la clase (el instructor falto, hubo una situacion de acoso, la clase no se
     * dicto como se prometio): eso afecta a todos los que pagaron, no al que ademas se tomo el
     * trabajo de reportarlo. Con el comportamiento viejo, los demas inscriptos no solo no
     * cobraban nada sino que su pago se liberaba igual al instructor apenas vencia el periodo
     * de denuncias.
     *
     * <p>Alcanza a los pagos {@code Retenido} <b>y</b> {@code Efectivo}, con el mismo criterio
     * que {@code cancelarclase} (decision 6): el efectivo no pasa por el gateway, asi que el
     * registro queda {@code Cancelado} y al alumno se le dice que coordine la devolucion.
     *
     * <p>A cada alumno se le notifica. Al denunciante le llega ademas la notificacion propia de
     * la resolucion de su denuncia, que manda {@code notificarResolucion}.
     */
    private void reintegrar(Denuncia denuncia) {
        Clase clase = denuncia.getClase();
        String contexto = "la clase de \"" + clase.getActividad().getNombre() + "\" del "
                + NotificacionMensajes.formatFechaHora(clase.getFechaHora());

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
                    inscripcion.getAlumno().getId(),
                    TipoNotificacion.INSCRIPCION_CANCELADA,
                    "Se resolvió un reclamo sobre " + contexto + " y se canceló tu inscripción." + detallePago,
                    clase.getId());
        }
    }

    /**
     * Suspender deja de ser solo un cambio de estado: el admin elige <b>cuántos días</b>
     * (mínimo {@link VentanaPenalizacion#MINIMO_DIAS_SUSPENSION}) y, opcionalmente, un
     * <b>monto</b> de multa. Se materializa como Penalizacion: la suspensión siempre, y la
     * económica solo si el monto es mayor a cero — las dos atadas a la denuncia que las
     * originó, así el listado puede mostrar "Ver denuncia" en ambas.
     *
     * <p>Antes esto ponía SUSPENDIDO y nada más: no quedaba constancia de por cuánto tiempo,
     * y el scheduler que levanta suspensiones vencidas no tenía qué vencer, así que la
     * suspensión era de hecho permanente.
     *
     * <p><b>Ya no toca {@code EstadoUsuario}</b>, por la misma decisión que {@code crearpenalizacion}:
     * el suspendido inicia sesión —tiene que poder ver su sanción y sus clases canceladas— pero
     * no puede operar, y eso lo hace valer {@code PenalizacionVigenteGuard} contra la vigencia.
     * Poner SUSPENDIDO acá le cerraba directamente el login.
     *
     * <p>Y <b>cancela las clases del instructor que caen dentro de la suspensión</b>, con
     * reintegro y aviso a cada alumno: un instructor que no puede dictar no puede dejar a sus
     * inscriptos esperando. La cascada va inline, como en el resto del repo.
     */
    private void suspenderInstructor(Denuncia denuncia, ResolverDenunciaRequest request, UUID actorId) {
        Integer dias = request.diasSuspension();
        if (dias == null) {
            throw new ValidacionException("Indicá cuántos días dura la suspensión.");
        }
        if (dias < VentanaPenalizacion.MINIMO_DIAS_SUSPENSION) {
            throw new ValidacionException(
                    "La suspensión no puede durar menos de " + VentanaPenalizacion.MINIMO_DIAS_SUSPENSION + " días.");
        }

        Usuario instructor = denuncia.getClase().getActividad().getInstructor();

        LocalDate desde = LocalDate.ofInstant(clock.instant(), Zonas.AR);
        Penalizacion suspension = new Penalizacion();
        suspension.setUsuario(instructor);
        suspension.setTipo(TipoPenalizacion.SUSPENSION_TEMPORAL);
        suspension.setMotivo(denuncia.getMotivo());
        suspension.setFechaInicio(desde);
        suspension.setFechaFin(desde.plusDays(dias));
        suspension.setDenuncia(denuncia);
        suspension = penalizacionRepository.saveAndFlush(suspension);
        int aplicadas = 1;

        auditService.registrar(
                actorId, AuditAccion.PENALIZACION_APLICADA, "Penalizacion", suspension.getId(),
                "SUSPENSION_TEMPORAL · " + dias + " días");

        BigDecimal monto = request.montoMulta();
        if (monto != null && monto.compareTo(BigDecimal.ZERO) > 0) {
            Penalizacion multa = new Penalizacion();
            multa.setUsuario(instructor);
            multa.setTipo(TipoPenalizacion.ECONOMICA);
            multa.setMotivo(denuncia.getMotivo());
            multa.setMonto(monto);
            multa.setDenuncia(denuncia);
            multa = penalizacionRepository.saveAndFlush(multa);
            aplicadas++;

            auditService.registrar(
                    actorId, AuditAccion.PENALIZACION_APLICADA, "Penalizacion", multa.getId(), "ECONOMICA");
        }

        instructor.setCantidadPenalizaciones(instructor.getCantidadPenalizaciones() + aplicadas);
        usuarioRepository.save(instructor);

        int clasesCanceladas = cancelarClasesDelPeriodo(instructor, desde, desde.plusDays(dias), actorId);

        // RN-14: la suspensión es una operación crítica y necesita su propio registro. Antes
        // solo quedaba la fila DENUNCIA_RESUELTA, sin rastro sobre el usuario sancionado.
        auditService.registrar(
                actorId, AuditAccion.USUARIO_ESTADO_ACTUALIZADO, "Usuario", instructor.getId(),
                "SUSPENSION_TEMPORAL · " + dias + " días · " + clasesCanceladas + " clases canceladas");
    }

    /**
     * Cancela las clases del instructor dentro de la vigencia de la suspensión y reintegra a
     * los inscriptos. Gemela de la de {@code crearpenalizacion}: las dos van inline porque la
     * convención del repo es no inyectar el Service de un usecase dentro de otro.
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
                        mensaje + detallePago, clase.getId());
            }

            clase.setEstado(EstadoClase.Cancelada);
            claseRepository.save(clase);
            auditService.registrar(actorId, AuditAccion.CLASE_CANCELADA, "Clase", clase.getId(), "PENALIZACION");
        }
        return clases.size();
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
