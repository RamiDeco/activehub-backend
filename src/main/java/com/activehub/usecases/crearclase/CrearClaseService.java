package com.activehub.usecases.crearclase;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.AgendaClases;
import com.activehub.domain.actividad.AgendaClasesRepository;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.favorito.FavoritoRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.security.PenalizacionVigenteGuard;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.security.InstructorVerificadoGuard;
import com.activehub.shared.time.Zonas;
import java.time.LocalDate;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearClaseService {

    private final ActividadRepository actividadRepository;
    private final ClaseRepository claseRepository;
    private final AgendaClasesRepository agendaClasesRepository;
    private final FavoritoRepository favoritoRepository;
    private final NotificacionService notificacionService;
    private final InstructorVerificadoGuard instructorVerificadoGuard;
    private final AuditService auditService;
    private final PenalizacionVigenteGuard penalizacionVigenteGuard;

    public CrearClaseService(
            ActividadRepository actividadRepository,
            ClaseRepository claseRepository,
            AgendaClasesRepository agendaClasesRepository,
            FavoritoRepository favoritoRepository,
            NotificacionService notificacionService,
            InstructorVerificadoGuard instructorVerificadoGuard,
            AuditService auditService,
            PenalizacionVigenteGuard penalizacionVigenteGuard
    ) {
        this.actividadRepository = actividadRepository;
        this.claseRepository = claseRepository;
        this.agendaClasesRepository = agendaClasesRepository;
        this.favoritoRepository = favoritoRepository;
        this.notificacionService = notificacionService;
        this.instructorVerificadoGuard = instructorVerificadoGuard;
        this.auditService = auditService;
        this.penalizacionVigenteGuard = penalizacionVigenteGuard;
    }

    @Transactional
    public CrearClaseResponse crear(UUID actividadId, CrearClaseRequest request, UUID instructorId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));

        if (!actividad.getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés crear clases para una actividad que no te pertenece.");
        }

        // RN-16 vía la guarda compartida; antes este slice tenía el chequeo copiado.
        instructorVerificadoGuard.exigirVerificado(instructorId, "crear clases");
        // Una suspension vigente corta la operacion, no la sesion: el penalizado entra y ve
        // lo suyo, pero no publica ni modifica oferta mientras dure la sancion.
        penalizacionVigenteGuard.exigirSinSuspensionVigente(instructorId, "crear clases");

        // La hora de fin se DERIVA de la duracion de la actividad (E2I-HU03 criterio 1: la
        // duracion es un dato de la actividad). El formulario pedia inicio y fin por separado,
        // que es el mismo dato dos veces y permitia crear una clase de 90 minutos para una
        // actividad que promete 60. El criterio 4 ("fin posterior al inicio") pasa a estar
        // garantizado por construccion: duracionMin tiene un CHECK > 0 en la base.
        Instant horaFin = request.fechaHora().plus(Duration.ofMinutes(actividad.getDuracionMin()));

        // Criterio 8: solapamiento con otra clase de la misma actividad.
        if (claseRepository.existeSolapamiento(actividadId, request.fechaHora(), horaFin, null)) {
            throw new ValidacionException(
                    "Ya existe una clase en ese horario. Modificá la fecha o el horario antes de continuar.");
        }

        ZonedDateTime inicioLocal = request.fechaHora().atZone(Zonas.AR);
        ZonedDateTime finLocal = horaFin.atZone(Zonas.AR);

        AgendaClases agenda = null;
        if (request.repetirSemanalmente()) {
            agenda = crearAgenda(actividad, request, inicioLocal, finLocal);
        }

        Clase clase = new Clase();
        clase.setActividad(actividad);
        clase.setFechaHora(request.fechaHora());
        clase.setHoraFin(horaFin);
        clase.setCuposMax(request.cuposMax());
        clase.setEstado(EstadoClase.Programada);
        clase.setCuposOcupados(0);
        // Precio propio desde el minuto cero (V23): lo que se le cobre a quien se anote no
        // puede cambiar porque despues se edite la actividad.
        clase.setPrecio(actividad.getPrecio());
        clase.setAgendaClases(agenda);
        clase = claseRepository.save(clase);

        auditService.registrar(instructorId, AuditAccion.CLASE_CREADA, "Clase", clase.getId(), null);

        String mensaje = "Se agregó un nuevo horario para \"" + actividad.getNombre()
                + "\" (uno de tus favoritos): " + NotificacionMensajes.formatFechaHora(clase.getFechaHora()) + ".";
        for (var favorito : favoritoRepository.findByActividadId(actividadId)) {
            notificacionService.notificar(
                    favorito.getUsuario().getId(), TipoNotificacion.NUEVO_HORARIO_FAVORITO, mensaje, clase.getId(),
                    // Al detalle de la actividad, que es donde puede anotarse al horario nuevo.
                    Destino.actividad(actividadId));
        }

        return new CrearClaseResponse(
                clase.getId(), actividad.getId(), clase.getFechaHora(), clase.getHoraFin(), clase.getEstado().name(),
                clase.getCuposMax(), clase.getCuposOcupados(), clase.getPrecio(),
                agenda != null ? agenda.getId() : null);
    }

    /**
     * Criterio 6: la recurrencia se guarda como AgendaClases, no como N clases creadas de golpe.
     * {@code MaterializarAgendasScheduler} instancia cada Clase una semana antes de dictarse,
     * "permitiendo darla de baja con anticipación".
     */
    private AgendaClases crearAgenda(
            Actividad actividad, CrearClaseRequest request, ZonedDateTime inicioLocal, ZonedDateTime finLocal) {
        LocalDate desde = inicioLocal.toLocalDate();
        // La agenda guarda horas sueltas (LocalTime), así que una clase que cruza la medianoche
        // no se puede expresar como recurrencia semanal. Se rechaza acá con un mensaje claro
        // en vez de dejar que explote el CHECK (hora_fin > hora_inicio) de la tabla.
        if (!finLocal.toLocalDate().equals(desde)) {
            throw new ValidacionException(
                    "Una clase que termina al día siguiente no se puede repetir cada semana.",
                    Map.of("repetirSemanalmente", "No disponible para clases que cruzan la medianoche."));
        }
        if (request.repetirHasta() != null && request.repetirHasta().isBefore(desde)) {
            throw new ValidacionException(
                    "La repetición no puede terminar antes de la primera clase.",
                    Map.of("repetirHasta", "Tiene que ser posterior a la fecha de la clase."));
        }

        AgendaClases agenda = new AgendaClases();
        agenda.setActividad(actividad);
        agenda.setDiaSemanaEnum(inicioLocal.getDayOfWeek());
        agenda.setHoraInicio(inicioLocal.toLocalTime());
        agenda.setHoraFin(finLocal.toLocalTime());
        agenda.setCuposMax(request.cuposMax());
        agenda.setVigenciaDesde(desde);
        agenda.setVigenciaHasta(request.repetirHasta());
        return agendaClasesRepository.save(agenda);
    }
}
