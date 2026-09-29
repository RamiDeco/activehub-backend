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
import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructor;
import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructorRepository;
import com.activehub.domain.mercadopago.EstadoCuentaMercadoPago;
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
import com.activehub.shared.payments.PaymentGateway;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InscribirseService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InscribirseService.class);

    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;
    private final NotificacionService notificacionService;
    private final InstructorVerificadoGuard instructorVerificadoGuard;
    private final CuentaMercadoPagoInstructorRepository cuentaMercadoPagoRepository;
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
            CuentaMercadoPagoInstructorRepository cuentaMercadoPagoRepository,
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
        this.cuentaMercadoPagoRepository = cuentaMercadoPagoRepository;
        this.clock = clock;
    }

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
            sellerToken = tokenDelInstructor(instructorId, ahora);
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
        String mensajeAlumno = inscripcion.getEstado() == EstadoInscripcion.INSCRIPTO
                ? "Se confirmó tu inscripción a " + contexto + "."
                : "Tu inscripción a " + contexto + " quedó pendiente hasta que el instructor confirme el pago en efectivo.";
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
     * El token OAuth del instructor dueno de la clase: la orden se crea en SU cuenta de MP.
     * Un token vencido no se intenta usar (el refresh es parte del OAuth, no de este caso de uso).
     */
    private String tokenDelInstructor(UUID instructorId, Instant ahora) {
        java.util.Optional<CuentaMercadoPagoInstructor> encontrada;
        try {
            encontrada = cuentaMercadoPagoRepository.findByInstructorId(instructorId);
        } catch (RuntimeException e) {
            // El token no se pudo descifrar (falta MP_TOKEN_ENCRYPTION_KEY o cambio la clave).
            // Hibernate puede envolver la excepcion del converter, por eso no se filtra por tipo.
            log.error("No se pudo leer la cuenta de Mercado Pago del instructor {}", instructorId, e);
            throw new PagoException("El pago con Mercado Pago no está disponible en este momento.");
        }
        CuentaMercadoPagoInstructor cuenta = encontrada
                .filter(c -> c.getEstado() == EstadoCuentaMercadoPago.VINCULADA)
                .filter(c -> c.getExpiresAt() == null || c.getExpiresAt().isAfter(ahora))
                .orElseThrow(() -> new PagoException(
                        "El instructor todavía no puede cobrar con Mercado Pago. Podés inscribirte pagando en efectivo."));
        return cuenta.getAccessToken();
    }

    /**
     * Autoriza (NO cobra) el precio de la clase: captura diferida, MP-01. Si MP rechaza, la
     * excepcion revierte toda la transaccion, cupo incluido.
     *
     * <p>La idempotency key sale de la inscripcion y del token de la tarjeta: el mismo pedido
     * reintentado (mismo token, que es de un solo uso) da la misma clave y MP devuelve la misma
     * orden en vez de reservar dos veces; un nuevo intento con otra tarjeta trae otro token y
     * por lo tanto otra clave.
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
        if (!resultado.autorizado()) {
            // "pending" u otro estado intermedio: sin autorizacion confirmada no se inscribe a
            // nadie. Si MP la confirma despues, vence sola a los 5 dias sin capturarse.
            throw new PagoException("Mercado Pago no confirmó la autorización del pago. Intentá nuevamente.");
        }

        pago.setEstado(EstadoPago.Retenido);
        pago.setMpOrderId(resultado.orderId());
        // Los casos de uso legacy (KAN-65) todavia operan por referenciaExterna.
        pago.setReferenciaExterna(resultado.orderId());
        pago.setStatusDetail(resultado.statusDetail());
        pago.setIdempotencyKey(idempotencyKey);
        pago.setComision(comision);
        pago.setMontoInstructor(pago.getMonto().subtract(comision));
        pago.setAutorizadoHasta(ahora.plus(VentanaPagos.VIGENCIA_AUTORIZACION));
    }
}
