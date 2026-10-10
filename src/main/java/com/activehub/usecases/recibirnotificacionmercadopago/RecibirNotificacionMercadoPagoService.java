package com.activehub.usecases.recibirnotificacionmercadopago;

import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.inscripcion.VentanaPagos;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.payments.CuentaMercadoPagoGuard;
import com.activehub.shared.payments.FirmaWebhookMercadoPago;
import com.activehub.shared.payments.OperacionesPagoMercadoPago;
import com.activehub.shared.payments.PaymentGateway;
import com.activehub.shared.payments.PaymentGateway.ResultadoPago;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Webhook de Mercado Pago (KAN-60): refleja en ActiveHub lo que cambio del lado de MP despues
 * de que respondimos al alumno.
 *
 * <p>Las cuatro reglas, en el orden en que se aplican:
 * <ol>
 *   <li><b>Firma:</b> sin una {@code x-signature} valida no se procesa nada (401). El endpoint es
 *       publico; sin esto cualquiera podria "aprobar" su propio pago.</li>
 *   <li><b>No se le cree al body:</b> se toma el id de la orden y se la CONSULTA a MP con el token
 *       del instructor. El body es una foto vieja y las notificaciones pueden llegar
 *       desordenadas; el estado actual de MP no.</li>
 *   <li><b>Idempotencia:</b> cada transicion parte de un estado concreto (p. ej. solo un pago
 *       {@code EnRevision} pasa a {@code Retenido}). MP reintenta y duplica avisos: la segunda vez
 *       el pago ya no esta en el estado de partida y no se hace nada, ni auditoria ni aviso.</li>
 *   <li><b>Responder 200 siempre que se pudo verificar:</b> un aviso de algo que no nos importa o
 *       que no es nuestro se ignora con 200. Si respondieramos error, MP lo reintentaria para
 *       siempre. Solo una falla al consultar a MP (transitoria) sale como error, para que MP
 *       reintente.</li>
 * </ol>
 */
@Service
public class RecibirNotificacionMercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(RecibirNotificacionMercadoPagoService.class);

    public enum Resultado { FIRMA_INVALIDA, IGNORADA, PROCESADA }

    private final FirmaWebhookMercadoPago firma;
    private final PagoRepository pagoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final ClaseRepository claseRepository;
    private final CuentaMercadoPagoGuard cuentaGuard;
    private final PaymentGateway paymentGateway;
    private final OperacionesPagoMercadoPago operacionesPago;
    private final AuditService auditService;
    private final NotificacionService notificacionService;
    private final Clock clock;

    public RecibirNotificacionMercadoPagoService(
            FirmaWebhookMercadoPago firma,
            PagoRepository pagoRepository,
            InscripcionRepository inscripcionRepository,
            ClaseRepository claseRepository,
            CuentaMercadoPagoGuard cuentaGuard,
            PaymentGateway paymentGateway,
            OperacionesPagoMercadoPago operacionesPago,
            AuditService auditService,
            NotificacionService notificacionService,
            Clock clock
    ) {
        this.firma = firma;
        this.pagoRepository = pagoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.claseRepository = claseRepository;
        this.cuentaGuard = cuentaGuard;
        this.paymentGateway = paymentGateway;
        this.operacionesPago = operacionesPago;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
        this.clock = clock;
    }

    @Transactional
    public Resultado procesar(RecibirNotificacionMercadoPagoRequest req) {
        // El sandbox de MP es inconsistente (doc formal): se deja rastro de TODO lo que llega.
        log.info("Webhook de Mercado Pago: type={} data.id={} x-request-id={}", req.type(), req.dataId(), req.xRequestId());

        if (!firma.esValida(req.xSignature(), req.xRequestId(), req.dataId())) {
            log.warn("Webhook de Mercado Pago con firma invalida (data.id={}): se descarta", req.dataId());
            return Resultado.FIRMA_INVALIDA;
        }
        if (!"order".equals(req.type()) || req.dataId() == null || req.dataId().isBlank()) {
            return Resultado.IGNORADA;
        }

        Pago pago = pagoRepository.findConDetalleByMpOrderId(req.dataId()).orElse(null);
        if (pago == null) {
            // Puede ser una orden de otra integracion o la simulacion del panel de MP.
            log.info("Webhook de Mercado Pago: la orden {} no corresponde a ningun pago", req.dataId());
            return Resultado.IGNORADA;
        }

        UUID instructorId = pago.getInscripcion().getClase().getActividad().getInstructor().getId();
        String token = cuentaGuard.exigirCuentaVinculada(instructorId);
        ResultadoPago orden = paymentGateway.consultar(token, pago.getMpOrderId());
        log.info("Webhook de Mercado Pago: orden {} en MP={}/{}, pago en ActiveHub={}",
                orden.orderId(), orden.status(), orden.statusDetail(), pago.getEstado());

        aplicar(pago, orden, instructorId);
        return Resultado.PROCESADA;
    }

    private void aplicar(Pago pago, ResultadoPago orden, UUID instructorId) {
        String status = orden.status();
        if (pago.getEstado() == EstadoPago.EnRevision) {
            if (orden.autorizado()) {
                aprobarEnRevision(pago, orden, instructorId);
            } else if ("failed".equals(status) || "canceled".equals(status) || "expired".equals(status)) {
                rechazarEnRevision(pago, orden);
            }
            // "processing"/"in_review": MP todavia no decidio. Nada que hacer.
            return;
        }
        if ("refunded".equals(status)
                && (pago.getEstado() == EstadoPago.Retenido || pago.getEstado() == EstadoPago.Liberado)) {
            // Reembolso hecho desde el panel de MP, por fuera de ActiveHub.
            pago.setEstado(EstadoPago.Reembolsado);
            pago.setStatusDetail(orden.statusDetail());
            pagoRepository.save(pago);
            auditService.registrar(null, AuditAccion.PAGO_REEMBOLSADO, "Pago", pago.getId(), "desde el panel de Mercado Pago");
            return;
        }
        if ("charged_back".equals(status)) {
            // Contracargo: el alumno desconocio el pago ante su banco. Se registra; resolverlo es
            // una decision de negocio (por ahora manual).
            log.warn("Contracargo en la orden {} (pago {}): {}", orden.orderId(), pago.getId(), orden.statusDetail());
            pago.setStatusDetail(orden.statusDetail());
            pagoRepository.save(pago);
        }
    }

    private void aprobarEnRevision(Pago pago, ResultadoPago orden, UUID instructorId) {
        Inscripcion inscripcion = pago.getInscripcion();
        if (inscripcion.getEstado() == EstadoInscripcion.CANCELADA) {
            // MP aprobo tarde: la inscripcion ya se cayo (la clase termino o el alumno se dio de
            // baja mientras estaba en revision). Se cancela la orden para no cobrar sin clase.
            log.warn("MP aprobo la orden {} pero la inscripcion {} ya esta cancelada: se cancela en MP",
                    orden.orderId(), inscripcion.getId());
            operacionesPago.reintegrar(pago, instructorId, null);
            return;
        }
        pago.setEstado(EstadoPago.Retenido);
        pago.setStatusDetail(orden.statusDetail());
        pago.setAutorizadoHasta(clock.instant().plus(VentanaPagos.VIGENCIA_AUTORIZACION));
        pagoRepository.save(pago);
        inscripcion.setEstado(EstadoInscripcion.INSCRIPTO);
        inscripcionRepository.save(inscripcion);

        auditService.registrar(null, AuditAccion.PAGO_CONFIRMADO_MP, "Pago", pago.getId(), orden.orderId());
        notificacionService.notificar(inscripcion.getAlumno().getId(), TipoNotificacion.INSCRIPCION_CONFIRMADA,
                "Mercado Pago aprobó tu pago: se confirmó tu inscripción a " + contexto(inscripcion) + ".",
                inscripcion.getId(), Destino.inscripcion(inscripcion.getId()));
    }

    private void rechazarEnRevision(Pago pago, ResultadoPago orden) {
        Inscripcion inscripcion = pago.getInscripcion();
        // Todo lo que se necesita se lee ANTES de liberar el cupo: liberarCupo limpia el
        // persistence context (clearAutomatically) y deja las entidades desconectadas.
        UUID alumnoId = inscripcion.getAlumno().getId();
        UUID inscripcionId = inscripcion.getId();
        UUID claseId = inscripcion.getClase().getId();
        String contexto = contexto(inscripcion);
        boolean ocupabaLugar = inscripcion.getEstado() == EstadoInscripcion.PAGO_PENDIENTE;

        pago.setEstado("failed".equals(orden.status()) ? EstadoPago.Rechazado : EstadoPago.Cancelado);
        pago.setStatusDetail(orden.statusDetail());
        pagoRepository.save(pago);
        if (ocupabaLugar) {
            inscripcion.setEstado(EstadoInscripcion.CANCELADA);
            inscripcionRepository.save(inscripcion);
            claseRepository.liberarCupo(claseId);
        }

        auditService.registrar(null, AuditAccion.PAGO_RECHAZADO_MP, "Pago", pago.getId(), orden.orderId());
        if (ocupabaLugar) {
            notificacionService.notificar(alumnoId, TipoNotificacion.INSCRIPCION_CANCELADA,
                    "Mercado Pago no aprobó tu pago para " + contexto + ", así que tu lugar se liberó. "
                            + "Podés volver a inscribirte con otra tarjeta o en efectivo.",
                    inscripcionId, Destino.inscripcion(inscripcionId));
        }
    }

    private static String contexto(Inscripcion inscripcion) {
        var clase = inscripcion.getClase();
        return "la clase de \"" + clase.getActividad().getNombre() + "\" del "
                + NotificacionMensajes.formatFechaHora(clase.getFechaHora());
    }
}
