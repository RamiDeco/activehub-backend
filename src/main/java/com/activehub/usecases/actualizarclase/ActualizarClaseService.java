package com.activehub.usecases.actualizarclase;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.security.PenalizacionVigenteGuard;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import java.util.Map;
import com.activehub.domain.inscripcion.VentanaInscripcion;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActualizarClaseService {

    private final ClaseRepository claseRepository;
    private final AuditService auditService;
    private final PenalizacionVigenteGuard penalizacionVigenteGuard;

    private final InstructorVerificadoGuard instructorVerificadoGuard;
    private final Clock clock;

    public ActualizarClaseService(
            ClaseRepository claseRepository,
            AuditService auditService,
            PenalizacionVigenteGuard penalizacionVigenteGuard,
            InstructorVerificadoGuard instructorVerificadoGuard,
            Clock clock
    ) {
        this.claseRepository = claseRepository;
        this.auditService = auditService;
        this.penalizacionVigenteGuard = penalizacionVigenteGuard;
        this.instructorVerificadoGuard = instructorVerificadoGuard;
        this.clock = clock;
    }

    @Transactional
    public ActualizarClaseResponse actualizar(UUID id, ActualizarClaseRequest request, UUID instructorId) {
        instructorVerificadoGuard.exigirVerificado(instructorId, "editar clases");
        // Una suspension vigente corta la operacion, no la sesion: el penalizado entra y ve
        // lo suyo, pero no publica ni modifica oferta mientras dure la sancion.
        penalizacionVigenteGuard.exigirSinSuspensionVigente(instructorId, "editar clases");
        Clase clase = claseRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Clase no encontrada."));

        if (!clase.getActividad().getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés modificar una clase que no te pertenece.");
        }

        // Clase congelada: ya entro a la ventana de inscripcion y hay al menos un alumno que
        // pago por estos datos. Cambiarle la fecha, el horario o el cupo a esa altura es
        // cambiarle el trato a alguien que ya cumplio su parte. El camino cuando la clase no
        // se puede dictar es cancelarla, que reintegra y avisa.
        Instant ahora = Instant.now(clock);
        if (VentanaInscripcion.estaCongelada(ahora, clase.getFechaHora(), clase.getCuposOcupados())) {
            throw new ValidacionException(
                    "Esta clase ya tiene inscriptos y está en período de inscripción: no se pueden modificar sus "
                            + "datos. Si no la vas a dictar, cancelala para que se reintegre el pago.");
        }

        if (request.cuposMax() < clase.getCuposOcupados()) {
            throw new ValidacionException(
                    "Los cupos máximos no pueden ser menores a los cupos ya ocupados.",
                    Map.of("cuposMax", "No puede ser menor a los cupos ya ocupados (" + clase.getCuposOcupados() + ")."));
        }

        // La hora de fin se deriva de la duracion de la actividad, igual que al crear: es el
        // mismo dato y no tiene sentido pedirlo dos veces. El criterio 4 ("fin posterior al
        // inicio") queda garantizado por construccion — duracionMin tiene un CHECK > 0.
        Instant horaFin = request.fechaHora().plus(
                Duration.ofMinutes(clase.getActividad().getDuracionMin()));

        // Criterio 8: el horario nuevo no puede pisar otra clase de la misma actividad.
        if (claseRepository.existeSolapamiento(
                clase.getActividad().getId(), request.fechaHora(), horaFin, clase.getId())) {
            throw new ValidacionException(
                    "Ya existe una clase en ese horario. Modificá la fecha o el horario antes de continuar.");
        }

        clase.setFechaHora(request.fechaHora());
        clase.setHoraFin(horaFin);
        clase.setCuposMax(request.cuposMax());
        claseRepository.save(clase);

        auditService.registrar(instructorId, AuditAccion.CLASE_ACTUALIZADA, "Clase", clase.getId(), null);

        return new ActualizarClaseResponse(
                clase.getId(), clase.getActividad().getId(), clase.getFechaHora(), clase.getHoraFin(),
                clase.getEstado().name(), clase.getCuposMax(), clase.getCuposOcupados());
    }
}
