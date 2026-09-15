package com.activehub.usecases.materializaragendas;

import com.activehub.domain.actividad.AgendaClases;
import com.activehub.domain.actividad.AgendaClasesRepository;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.favorito.FavoritoRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.time.Zonas;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E2I-HU06 criterio 6: "El sistema creará cada instancia automáticamente <b>una semana antes</b>
 * de su dictado, permitiendo darla de baja con anticipación".
 *
 * <p>Es la contrapartida de {@link AgendaClases}: la agenda define la recurrencia y este job
 * materializa la {@link Clase} concreta cuando entra en la ventana de una semana. Generar las N
 * clases de una al crear la agenda seria lo facil, pero contradice el criterio y llenaria el
 * calendario de clases a un año vista que nadie puede cancelar de a una.
 */
@Service
public class MaterializarAgendasService {

    /** Se instancia la clase cuando faltan 7 dias o menos para dictarla. */
    static final Duration ANTICIPACION = Duration.ofDays(7);

    private final AgendaClasesRepository agendaClasesRepository;
    private final ClaseRepository claseRepository;
    private final FavoritoRepository favoritoRepository;
    private final NotificacionService notificacionService;
    private final AuditService auditService;
    private final Clock clock;

    public MaterializarAgendasService(
            AgendaClasesRepository agendaClasesRepository,
            ClaseRepository claseRepository,
            FavoritoRepository favoritoRepository,
            NotificacionService notificacionService,
            AuditService auditService,
            Clock clock
    ) {
        this.agendaClasesRepository = agendaClasesRepository;
        this.claseRepository = claseRepository;
        this.favoritoRepository = favoritoRepository;
        this.notificacionService = notificacionService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public int materializar() {
        Instant ahora = clock.instant();
        LocalDate hoy = LocalDate.ofInstant(ahora, Zonas.AR);
        Instant limite = ahora.plus(ANTICIPACION);

        int creadas = 0;
        for (AgendaClases agenda : agendaClasesRepository.findVigentesConDetalle(hoy)) {
            LocalDate fecha = proximaFecha(agenda, hoy);
            // Una agenda semanal puede tener a lo sumo una ocurrencia dentro de 7 dias,
            // pero se itera igual por si la ventana se agranda mas adelante.
            while (fecha != null && agenda.vigenteEn(fecha)) {
                Instant inicio = ZonedDateTime.of(fecha, agenda.getHoraInicio(), Zonas.AR).toInstant();
                if (inicio.isAfter(limite)) {
                    break;
                }
                if (inicio.isAfter(ahora) && crearSiFalta(agenda, fecha, inicio)) {
                    creadas++;
                }
                fecha = fecha.plusWeeks(1);
            }
        }
        return creadas;
    }

    private LocalDate proximaFecha(AgendaClases agenda, LocalDate hoy) {
        LocalDate desde = agenda.getVigenciaDesde().isAfter(hoy) ? agenda.getVigenciaDesde() : hoy;
        return desde.with(TemporalAdjusters.nextOrSame(agenda.getDiaSemanaEnum()));
    }

    private boolean crearSiFalta(AgendaClases agenda, LocalDate fecha, Instant inicio) {
        // Idempotencia: el job corre cada hora, no puede duplicar la clase de la semana.
        if (claseRepository.existsByAgendaClasesIdAndFechaHora(agenda.getId(), inicio)) {
            return false;
        }
        Instant fin = ZonedDateTime.of(fecha, agenda.getHoraFin(), Zonas.AR).toInstant();
        // La agenda no puede pisar una clase suelta que el instructor haya cargado a mano.
        if (claseRepository.existeSolapamiento(agenda.getActividad().getId(), inicio, fin, null)) {
            return false;
        }

        Clase clase = new Clase();
        clase.setActividad(agenda.getActividad());
        clase.setAgendaClases(agenda);
        clase.setFechaHora(inicio);
        clase.setHoraFin(fin);
        clase.setCuposMax(agenda.getCuposMax());
        clase.setEstado(EstadoClase.Programada);
        clase.setCuposOcupados(0);
        // El precio se fija cuando la clase se materializa, no cuando se creo la agenda: es el
        // precio vigente de la actividad en el momento en que la clase empieza a existir y a
        // poder venderse (V23).
        clase.setPrecio(agenda.getActividad().getPrecio());
        clase = claseRepository.save(clase);

        auditService.registrar(null, AuditAccion.CLASE_CREADA, "Clase", clase.getId(), "AGENDA");

        String mensaje = "Se agregó un nuevo horario para \"" + agenda.getActividad().getNombre()
                + "\" (uno de tus favoritos): " + NotificacionMensajes.formatFechaHora(inicio) + ".";
        for (var favorito : favoritoRepository.findByActividadId(agenda.getActividad().getId())) {
            notificacionService.notificar(
                    favorito.getUsuario().getId(), TipoNotificacion.NUEVO_HORARIO_FAVORITO, mensaje, clase.getId());
        }
        return true;
    }
}
