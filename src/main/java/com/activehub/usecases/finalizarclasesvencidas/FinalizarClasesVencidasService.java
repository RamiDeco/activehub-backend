package com.activehub.usecases.finalizarclasesvencidas;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.inscripcion.VentanaPagos;
import com.activehub.domain.permiso.ConfiguracionRolRepository;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.payments.OperacionesPagoMercadoPago;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalizarClasesVencidasService {

    private static final Logger log = LoggerFactory.getLogger(FinalizarClasesVencidasService.class);

    private static final List<EstadoClase> ESTADOS_A_FINALIZAR = List.of(EstadoClase.Programada, EstadoClase.Habilitada);

    /** KAN-61: quien tiene este permiso recibe el aviso de un pago que no se puede capturar. */
    static final String PERMISO_AVISO_CAPTURA = "auditoria.ver";

    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;
    private final OperacionesPagoMercadoPago operacionesPago;
    private final AuditService auditService;
    private final Clock clock;
    private final ConfiguracionRolRepository configuracionRolRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public FinalizarClasesVencidasService(
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            PagoRepository pagoRepository,
            OperacionesPagoMercadoPago operacionesPago,
            AuditService auditService,
            Clock clock,
            ConfiguracionRolRepository configuracionRolRepository,
            UsuarioRepository usuarioRepository,
            NotificacionService notificacionService
    ) {
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.operacionesPago = operacionesPago;
        this.auditService = auditService;
        this.clock = clock;
        this.configuracionRolRepository = configuracionRolRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public void finalizar() {
        List<Clase> vencidas = claseRepository.findByEstadoInAndFechaHoraBefore(ESTADOS_A_FINALIZAR, clock.instant());

        for (Clase clase : vencidas) {
            clase.setEstado(EstadoClase.Finalizada);
            claseRepository.save(clase);

            for (Inscripcion inscripcion : inscripcionRepository.findByClaseIdAndEstadoNot(clase.getId(), EstadoInscripcion.CANCELADA)) {
                if (inscripcion.getEstado() == EstadoInscripcion.PRE_INSCRIPCION
                        || inscripcion.getEstado() == EstadoInscripcion.PAGO_PENDIENTE) {
                    inscripcion.setEstado(EstadoInscripcion.CANCELADA);
                    inscripcionRepository.save(inscripcion);
                }
            }

            auditService.registrar(null, AuditAccion.CLASE_FINALIZADA, "Clase", clase.getId(), null);
        }
    }

    /**
     * KAN-65: captura en Mercado Pago los pagos autorizados de las clases ya Finalizadas. Va
     * despues de {@link #finalizar()} y no lo reemplaza: no mira solo las recien finalizadas sino
     * todas las que quedaron sin capturar, asi una captura que fallo se reintenta en la proxima
     * corrida (cada 5 minutos) hasta que entra o vence la autorizacion.
     *
     * <p>KAN-61 suma dos cosas:
     * <ul>
     *   <li><b>Captura anticipada:</b> tambien entran los pagos de clases sin finalizar cuya
     *       autorizacion vence dentro de {@link VentanaPagos#MARGEN_CAPTURA_ANTICIPADA}. Si
     *       despues hay una denuncia o la clase se cancela, el pago ya capturado se reembolsa
     *       ({@code OperacionesPagoMercadoPago.reintegrar} lo resuelve solo).</li>
     *   <li><b>Registro de la falla:</b> una captura fallida suma un intento, se audita y, al
     *       llegar a {@link VentanaPagos#MAX_INTENTOS_CAPTURA}, se avisa a la administracion.</li>
     * </ul>
     *
     * <p>Sin {@code @Transactional} a proposito: cada pago commitea solo. Dentro de una
     * transaccion comun, una falla de MP en un pago (o de la lectura del token del instructor,
     * que marca la transaccion para rollback aunque se la atrape) revertiria las capturas de los
     * demas, que en MP ya se hicieron.
     *
     * @return cuantos pagos se intentaron sin error
     */
    public int capturarPagosPendientes() {
        Instant ahora = clock.instant();
        List<Pago> finalizadas = pagoRepository.findSinCapturarDeClasesEn(EstadoPago.Retenido, EstadoClase.Finalizada);
        List<Pago> porVencer = pagoRepository.findSinCapturarPorVencer(
                EstadoPago.Retenido, ahora.plus(VentanaPagos.MARGEN_CAPTURA_ANTICIPADA), ESTADOS_A_FINALIZAR);

        int capturados = 0;
        for (Pago pago : finalizadas) {
            if (intentarCaptura(pago, false)) {
                capturados++;
            }
        }
        for (Pago pago : porVencer) {
            if (intentarCaptura(pago, true)) {
                capturados++;
            }
        }
        return capturados;
    }

    private boolean intentarCaptura(Pago pago, boolean anticipada) {
        var instructorId = pago.getInscripcion().getClase().getActividad().getInstructor().getId();
        // Se lee antes: capturar() lo pone en null al cobrar.
        Instant vence = pago.getAutorizadoHasta();
        try {
            operacionesPago.capturar(pago, instructorId);
        } catch (RuntimeException e) {
            registrarFalla(pago, e);
            return false;
        }
        if (anticipada && pago.getCapturadoAt() != null) {
            log.warn("Pago {} capturado antes de finalizar la clase: la autorizacion vencia el {}", pago.getId(), vence);
            auditService.registrar(null, AuditAccion.PAGO_CAPTURA_ANTICIPADA, "Pago", pago.getId(), "vencia " + vence);
        }
        return true;
    }

    /**
     * El pago sigue Retenido y sin capturar: la proxima corrida lo vuelve a intentar. En la
     * auditoria no va el mensaje de la excepcion, que puede traer pedazos de la respuesta de MP.
     *
     * <p>Nada de lo que pasa aca puede salir del metodo: se ejecuta dentro del catch de
     * {@link #intentarCaptura}, y una excepcion cortaria el bucle y dejaria sin intentar al resto
     * de los pagos de la corrida.
     */
    private void registrarFalla(Pago pago, RuntimeException e) {
        int intento = pago.getIntentosCaptura() + 1;
        log.error("No se pudo capturar el pago {} (orden {}, intento {}): se reintenta en la proxima corrida",
                pago.getId(), pago.getMpOrderId(), intento, e);
        try {
            pago.setIntentosCaptura(intento);
            pagoRepository.save(pago);
            auditService.registrar(null, AuditAccion.PAGO_CAPTURA_FALLIDA, "Pago", pago.getId(),
                    "intento " + intento + " · " + e.getClass().getSimpleName());
        } catch (RuntimeException errorAlRegistrar) {
            log.error("No se pudo registrar la falla de captura del pago {}", pago.getId(), errorAlRegistrar);
            return;
        }

        // Igual y no mayor o igual: se avisa una sola vez, no en cada corrida de ahi en adelante.
        if (intento == VentanaPagos.MAX_INTENTOS_CAPTURA) {
            avisarAdministracion(pago);
        }
    }

    /**
     * KAN-61: avisa a quienes tienen {@link #PERMISO_AVISO_CAPTURA} que este pago no se pudo
     * capturar. Se llama una sola vez, al llegar al tope de intentos. Sin destino: la
     * administracion no tiene una pantalla de pagos a la que llevar el click.
     */
    private void avisarAdministracion(Pago pago) {
        log.error("El pago {} llego a {} capturas fallidas: requiere intervencion de un administrador",
                pago.getId(), VentanaPagos.MAX_INTENTOS_CAPTURA);
        try {
            List<UUID> roles = configuracionRolRepository.rolesConPermiso(PERMISO_AVISO_CAPTURA);
            // Con la lista vacia el IN de la consulta no es valido en Postgres.
            if (roles.isEmpty()) {
                return;
            }

            var clase = pago.getInscripcion().getClase();
            String mensaje = "No se pudo cobrar en Mercado Pago un pago de la clase de "
                    + clase.getActividad().getNombre() + " del "
                    + NotificacionMensajes.formatFechaHora(clase.getFechaHora())
                    + " después de " + VentanaPagos.MAX_INTENTOS_CAPTURA + " intentos. "
                    + "Revisá que el instructor tenga su cuenta de Mercado Pago vinculada.";

            for (UUID adminId : usuarioRepository.findIdsActivosPorRol(roles)) {
                notificacionService.notificar(adminId, TipoNotificacion.CAPTURA_PAGO_FALLIDA, mensaje,
                        pago.getId(), Destino.ninguno());
            }
        } catch (RuntimeException e) {
            log.error("No se pudo avisar a la administración del pago {}", pago.getId(), e);
        }
    }
}
