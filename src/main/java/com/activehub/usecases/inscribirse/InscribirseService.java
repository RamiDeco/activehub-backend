package com.activehub.usecases.inscribirse;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.MetodoPago;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.inscripcion.VentanaInscripcion;
import com.activehub.domain.inscripcion.VentanaPagos;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.InscripcionYaExisteException;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.PagoException;
import com.activehub.shared.error.SinCuposDisponiblesException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.payments.CuentaMercadoPagoGuard;
import com.activehub.shared.payments.PaymentGateway;
import com.activehub.shared.security.InstructorVerificadoGuard;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class InscribirseService {

    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;
    private final NotificacionService notificacionService;
    private final InstructorVerificadoGuard instructorVerificadoGuard;
    private final CuentaMercadoPagoGuard cuentaMercadoPagoGuard;
    private final Clock clock;

    public InscribirseService(
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            PagoRepository pagoRepository,
            UsuarioRepository usuarioRepository,
            PaymentGateway paymentGateway,
            AuditService auditService,
            NotificacionService notificacionService,
            InstructorVerificadoGuard instructorVerificadoGuard,
            CuentaMercadoPagoGuard cuentaMercadoPagoGuard,
            Clock clock
    ) {
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.paymentGateway = paymentGateway;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
        this.instructorVerificadoGuard = instructorVerificadoGuard;
        this.cuentaMercadoPagoGuard = cuentaMercadoPagoGuard;
        this.clock = clock;
    }

    private static final Logger log = LoggerFactory.getLogger(InscribirseService.class);

    @Transactional
    public InscribirseResponse inscribirse(UUID claseId, InscribirseRequest request, UUID alumnoId) {
        Clase clase = claseRepository.findById(claseId)
                .orElseThrow(() -> new NoEncontradoException("Clase no encontrada."));

        if (clase.getEstado() == EstadoClase.Cancelada || clase.getEstado() == EstadoClase.Finalizada) {
            throw new ValidacionException("Esta clase ya no admite inscripciones.");
        }
        // RN-16: si al instructor le revocaron la verificación, su oferta deja de ser reservable.
        instructorVerificadoGuard.exigirOfertaVigente(clase.getActividad().getInstructor().getId());

        var ahora = clock.instant();
        if (!VentanaInscripcion.esVentanaInscripcion(ahora, clase.getFechaHora())) {
            if (VentanaInscripcion.esVentanaPreInscripcion(ahora, clase.getFechaHora())) {
                throw new ValidacionException(
                        "Todavía faltan más de 4 días para la clase: por ahora solo podés preinscribirte.");
            }
            throw new ValidacionException(
                    "Ya pasó el límite para inscribirse (1 hora antes del inicio de la clase).");
        }

        Inscripcion existente = inscripcionRepository
                .findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA)
                .orElse(null);

        if (existente != null && existente.getEstado() != EstadoInscripcion.PRE_INSCRIPCION) {
            throw new InscripcionYaExisteException("Ya tenés una inscripción activa para esta clase.");
        }

        // Leer el precio y los datos de la actividad ANTES del UPDATE atomico: ocuparCupo usa
        // clearAutomatically=true, que limpia todo el persistence context (no solo Clase) y
        // dejaria el proxy lazy de Actividad huerfano si se accede despues.
        // El precio sale de la CLASE, no de la actividad (V23): la actividad puede haber
        // cambiado de precio despues de que esta clase se publicara.
        var precio = clase.getPrecio();
        var actividadNombre = clase.getActividad().getNombre();
        var instructorId = clase.getActividad().getInstructor().getId();
        var fechaHoraClase = clase.getFechaHora();

        // Todo lo que puede rechazar un pago con Mercado Pago sin llamar a MP va ANTES de ocupar
        // el cupo: faltan datos o el instructor no puede cobrar.
        MetodoPago metodo = MetodoPago.fromEtiqueta(request.metodoPago());
        String sellerToken = null;
        if (metodo == MetodoPago.MERCADO_PAGO) {
            if (request.datosPago() == null) {
                throw new ValidacionException("Los datos de pago son obligatorios para Mercado Pago.");
            }
            // KAN-57: sin cuenta vinculada la clase solo acepta Efectivo (409).
            sellerToken = cuentaMercadoPagoGuard.exigirCuentaVinculada(instructorId);
        }

        if (claseRepository.ocuparCupo(claseId) == 0) {
            throw new SinCuposDisponiblesException();
        }

        boolean esUpgrade = existente != null;

        Inscripcion inscripcion = esUpgrade ? existente : new Inscripcion();
        if (!esUpgrade) {
            inscripcion.setClase(clase);
            inscripcion.setAlumno(usuarioRepository.getReferenceById(alumnoId));
        }
        inscripcion.setEstado(metodo == MetodoPago.MERCADO_PAGO ? EstadoInscripcion.INSCRIPTO : EstadoInscripcion.PAGO_PENDIENTE);
        inscripcion = inscripcionRepository.saveAndFlush(inscripcion);

        Pago pago = new Pago();
        pago.setInscripcion(inscripcion);
        pago.setMonto(precio);
        pago.setMetodo(metodo);

        if (metodo == MetodoPago.MERCADO_PAGO) {
            autorizarConMercadoPago(pago, request.datosPago(), sellerToken, alumnoId, ahora);
        } else {
            pago.setEstado(EstadoPago.Efectivo);
            pago.setMontoInstructor(precio);
        }
        pago = pagoRepository.save(pago);

        String contexto = "la clase de \"" + actividadNombre + "\" del " + NotificacionMensajes.formatFechaHora(fechaHoraClase);
        String mensajeAlumno;
        if (inscripcion.getEstado() == EstadoInscripcion.INSCRIPTO) {
            mensajeAlumno = "Se confirmó tu inscripción a " + contexto + ".";
        } else if (pago.getEstado() == EstadoPago.EnRevision) {
            mensajeAlumno = "Tu lugar en " + contexto + " está reservado: Mercado Pago está revisando tu pago y te "
                    + "avisamos apenas se confirme.";
        } else {
            mensajeAlumno = "Tu inscripción a " + contexto + " quedó pendiente hasta que el instructor confirme el pago en efectivo.";
        }
        notificacionService.notificar(
                alumnoId, TipoNotificacion.INSCRIPCION_CONFIRMADA, mensajeAlumno, inscripcion.getId(),
                Destino.inscripcion(inscripcion.getId()));
        notificacionService.notificar(
                instructorId, TipoNotificacion.NUEVA_INSCRIPCION, "Nueva inscripción en " + contexto + ".", inscripcion.getId(),
                // Al roster de la clase: es donde ve quien se anoto y donde confirma el cobro.
                Destino.clase(claseId));

        auditService.registrar(
                alumnoId,
                esUpgrade ? AuditAccion.INSCRIPCION_ACTUALIZADA : AuditAccion.INSCRIPCION_CREADA,
                "Inscripcion", inscripcion.getId(), null);

        return new InscribirseResponse(
                inscripcion.getId(), claseId, alumnoId, inscripcion.getEstado().getEtiqueta(),
                inscripcion.getCreatedAt(), pago.getId());
    }

    /**
     * Autoriza (NO cobra) el precio de la clase: captura diferida, MP-01. Si MP
     * rechaza, la excepcion revierte toda la transaccion, cupo incluido.
     *
     * <p>
     * La idempotency key sale de la inscripcion y del token de la tarjeta: el
     * mismo pedido reintentado (mismo token, que es de un solo uso) da la misma
     * clave y MP devuelve la misma orden en vez de reservar dos veces; un nuevo
     * intento con otra tarjeta trae otro token y por lo tanto otra clave.
     */
    private void autorizarConMercadoPago(Pago pago, DatosPagoRequest datos, String sellerToken, UUID alumnoId,
            Instant ahora) {
        BigDecimal comision = BigDecimal.ZERO; // MP-01: la plataforma no cobra comision
        String idempotencyKey = UUID.nameUUIDFromBytes(
                ("autorizar:" + pago.getInscripcion().getId() + ":" + datos.token()).getBytes(StandardCharsets.UTF_8))
                .toString();
        var tarjeta = new PaymentGateway.DatosTarjeta(
                datos.token(), datos.paymentMethodId(), datos.paymentMethodType(), datos.cuotas(),
                usuarioRepository.findById(alumnoId).map(u -> u.getEmail()).orElse(null));

        var resultado = paymentGateway.autorizar(
                sellerToken, tarjeta, pago.getMonto(), comision, pago.getInscripcion().getId().toString(), idempotencyKey);
        boolean enRevision = resultado.enRevision();
        if (!resultado.autorizado() && !enRevision) {
            // Un estado que no es ni autorizado, ni rechazado (eso ya lanzo), ni en revision: no
            // se inscribe a nadie. Si igual quedara algo reservado, vence solo a los 5 dias.
            throw new PagoException("Mercado Pago no confirmó la autorización del pago. Intentá nuevamente.");
        }

        String orderId = resultado.orderId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                try {
                    String cancelIdempotencyKey = idempotencyKey + ":cancel";
                    paymentGateway.cancelarAutorizacion(sellerToken, orderId, cancelIdempotencyKey);
                } catch (RuntimeException e) {
                    log.error("No se pudo cancelar la autorización de Mercado Pago para la orden {} tras el rollback: quedó AUTORIZADA sin inscripción, cancelarla a mano en Mercado Pago", orderId, e);
                }
            }
        });

        if (enRevision) {
            // KAN-58: MP no decidio todavia. El lugar queda reservado (PagoPendiente) pero el
            // instructor no puede confirmarlo a mano: lo resuelve el webhook (KAN-60). Sin
            // autorizado_hasta, porque todavia no hay autorizacion que venza.
            pago.setEstado(EstadoPago.EnRevision);
            pago.getInscripcion().setEstado(EstadoInscripcion.PAGO_PENDIENTE);
        } else {
            pago.setEstado(EstadoPago.Retenido);
            pago.setAutorizadoHasta(ahora.plus(VentanaPagos.VIGENCIA_AUTORIZACION));
        }
        pago.setMpOrderId(resultado.orderId());
        // Mismo valor que mpOrderId; se conserva por trazabilidad con los pagos previos a MP-04.
        pago.setReferenciaExterna(resultado.orderId());
        pago.setStatusDetail(resultado.statusDetail());
        pago.setIdempotencyKey(idempotencyKey);
        pago.setComision(comision);
        pago.setMontoInstructor(pago.getMonto().subtract(comision));
    }
}
