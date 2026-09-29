package com.activehub.usecases.eliminaractividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.security.PenalizacionVigenteGuard;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.ActividadConInscriptosException;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.util.EnumSet;
import java.util.List;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarActividadService {

    private final ActividadRepository actividadRepository;
    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AuditService auditService;
    private final PenalizacionVigenteGuard penalizacionVigenteGuard;
    private final NotificacionService notificacionService;

    private final InstructorVerificadoGuard instructorVerificadoGuard;

    public EliminarActividadService(
            ActividadRepository actividadRepository,
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            AuditService auditService,
            PenalizacionVigenteGuard penalizacionVigenteGuard,
            NotificacionService notificacionService,
            InstructorVerificadoGuard instructorVerificadoGuard
    ) {
        this.actividadRepository = actividadRepository;
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.auditService = auditService;
        this.penalizacionVigenteGuard = penalizacionVigenteGuard;
        this.notificacionService = notificacionService;
        this.instructorVerificadoGuard = instructorVerificadoGuard;
    }

    @Transactional
    public void eliminar(UUID id, UUID actorId, boolean puedeModerar) {
        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));

        if (!puedeModerar && !actividad.getInstructor().getId().equals(actorId)) {
            throw new SinPermisoException("No podés eliminar una actividad que no te pertenece.");
        }
        // El admin no necesita perfil de instructor verificado: la guarda solo aplica
        // cuando el que borra es el propio instructor.
        if (!puedeModerar) {
            instructorVerificadoGuard.exigirVerificado(actorId, "eliminar actividades");
            // Solo para el dueño: un moderador no esta operando SU oferta, y si un admin queda
            // penalizado eso no deberia bloquear la moderacion de la plataforma.
            penalizacionVigenteGuard.exigirSinSuspensionVigente(actorId, "eliminar actividades");
        }

        List<Clase> clasesVigentes = claseRepository.findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(
                id, EnumSet.of(EstadoClase.Cancelada, EstadoClase.Finalizada));

        // E2I-HU06 criterio 7: con inscriptos o pagos pendientes NO se borra nada; el
        // instructor tiene que cancelar esas clases primero (cancelarclase es el camino que
        // notifica y reintegra). Antes esto cascadeaba en silencio: un botón de "Eliminar"
        // terminaba cancelando inscripciones pagas sin que nadie lo pidiera explícitamente.
        boolean hayCompromisos = clasesVigentes.stream().anyMatch(clase ->
                !inscripcionRepository.findByClaseIdAndEstadoNot(clase.getId(), EstadoInscripcion.CANCELADA).isEmpty());
        if (hayCompromisos) {
            throw new ActividadConInscriptosException();
        }

        // Llegado acá ninguna clase vigente tiene gente anotada: se cancelan vacías para que
        // no queden colgadas de una actividad dada de baja.
        for (Clase clase : clasesVigentes) {
            clase.setEstado(EstadoClase.Cancelada);
            claseRepository.save(clase);

            auditService.registrar(actorId, AuditAccion.CLASE_CANCELADA, "Clase", clase.getId(), null);
        }

        boolean loBorraUnModerador = puedeModerar && !actividad.getInstructor().getId().equals(actorId);
        if (loBorraUnModerador) {
            notificacionService.notificar(
                    actividad.getInstructor().getId(),
                    TipoNotificacion.ACTIVIDAD_ELIMINADA,
                    "Un administrador eliminó tu actividad \"" + actividad.getNombre() + "\".",
                    // Sin destino: la actividad ya no existe, el link solo llevaria a un 404.
                    actividad.getId(), Destino.ninguno());
        }

        actividad.marcarBorrado();
        actividadRepository.save(actividad);

        auditService.registrar(actorId, AuditAccion.ACTIVIDAD_ELIMINADA, "Actividad", id, null);
    }
}
