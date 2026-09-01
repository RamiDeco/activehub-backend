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
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.InscripcionYaExisteException;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinCuposDisponiblesException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.payments.PaymentGateway;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InscribirseService {

    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;
    private final NotificacionService notificacionService;
    private final Clock clock;

    public InscribirseService(
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            PagoRepository pagoRepository,
            UsuarioRepository usuarioRepository,
            PaymentGateway paymentGateway,
            AuditService auditService,
            NotificacionService notificacionService,
            Clock clock
    ) {
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.paymentGateway = paymentGateway;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
        this.clock = clock;
    }

    @Transactional
    public InscribirseResponse inscribirse(UUID claseId, InscribirseRequest request, UUID alumnoId) {
        Clase clase = claseRepository.findById(claseId)
                .orElseThrow(() -> new NoEncontradoException("Clase no encontrada."));

        if (clase.getEstado() == EstadoClase.Cancelada || clase.getEstado() == EstadoClase.Finalizada) {
            throw new ValidacionException("Esta clase ya no admite inscripciones.");
        }

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
        var precio = clase.getActividad().getPrecio();
        var actividadNombre = clase.getActividad().getNombre();
        var instructorId = clase.getActividad().getInstructor().getId();
        var fechaHoraClase = clase.getFechaHora();

        if (claseRepository.ocuparCupo(claseId) == 0) {
            throw new SinCuposDisponiblesException();
        }

        MetodoPago metodo = MetodoPago.fromEtiqueta(request.metodoPago());
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
            long montoEnCentavos = pago.getMonto().movePointRight(2).longValueExact();
            var resultado = paymentGateway.iniciarPago(inscripcion.getId().toString(), montoEnCentavos);
            pago.setReferenciaExterna(resultado.referenciaExterna());
            pago.setEstado(EstadoPago.Retenido);
        } else {
            pago.setEstado(EstadoPago.Efectivo);
        }
        pago = pagoRepository.save(pago);

        String contexto = "la clase de \"" + actividadNombre + "\" del " + NotificacionMensajes.formatFechaHora(fechaHoraClase);
        String mensajeAlumno = inscripcion.getEstado() == EstadoInscripcion.INSCRIPTO
                ? "Se confirmó tu inscripción a " + contexto + "."
                : "Tu inscripción a " + contexto + " quedó pendiente hasta que el instructor confirme el pago en efectivo.";
        notificacionService.notificar(alumnoId, TipoNotificacion.INSCRIPCION_CONFIRMADA, mensajeAlumno, inscripcion.getId());
        notificacionService.notificar(
                instructorId, TipoNotificacion.NUEVA_INSCRIPCION, "Nueva inscripción en " + contexto + ".", inscripcion.getId());

        auditService.registrar(
                alumnoId,
                esUpgrade ? AuditAccion.INSCRIPCION_ACTUALIZADA : AuditAccion.INSCRIPCION_CREADA,
                "Inscripcion", inscripcion.getId(), null);

        return new InscribirseResponse(
                inscripcion.getId(), claseId, alumnoId, inscripcion.getEstado().getEtiqueta(),
                inscripcion.getCreatedAt(), pago.getId());
    }
}
