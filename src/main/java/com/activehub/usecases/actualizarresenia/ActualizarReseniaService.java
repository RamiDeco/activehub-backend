package com.activehub.usecases.actualizarresenia;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E3A-HU10 criterios 1 y 6: editar una reseña propia.
 *
 * <p>Antes no existia mutador de edicion, asi que la pantalla "editaba" borrando la reseña y
 * creando otra: dos requests sin transaccion, con lo cual si el alta fallaba despues de la
 * baja el alumno perdia su reseña. Ademas cambiaba el id, la fecha y le mandaba al instructor
 * una segunda notificacion de "reseña nueva".
 *
 * <p>Al editar vuelve a moderacion: el contenido cambio y el admin todavia no lo aprobo.
 */
@Service
public class ActualizarReseniaService {

    private final ReseniaRepository reseniaRepository;
    private final ActividadRepository actividadRepository;
    private final AuditService auditService;

    public ActualizarReseniaService(
            ReseniaRepository reseniaRepository,
            ActividadRepository actividadRepository,
            AuditService auditService
    ) {
        this.reseniaRepository = reseniaRepository;
        this.actividadRepository = actividadRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ActualizarReseniaResponse actualizar(UUID id, ActualizarReseniaRequest request, UUID alumnoId) {
        Resenia resenia = reseniaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Reseña no encontrada."));

        if (!resenia.getAlumno().getId().equals(alumnoId)) {
            throw new SinPermisoException("No podés editar una reseña que no te pertenece.");
        }

        resenia.setPuntaje(request.puntaje());
        resenia.setComentario(normalizar(request.comentario()));
        resenia.setEnModeracion(true);
        reseniaRepository.save(resenia);

        // El puntaje cambió: hay que recalcular el rating de la actividad.
        actividadRepository.recalcularRating(resenia.getClase().getActividad().getId());

        auditService.registrar(alumnoId, AuditAccion.RESENIA_ACTUALIZADA, "Resenia", id, null);

        return new ActualizarReseniaResponse(
                resenia.getId(),
                resenia.getClase().getId(),
                resenia.getPuntaje(),
                resenia.getComentario(),
                resenia.isEnModeracion());
    }

    /** Un comentario en blanco es "sin comentario", no una cadena vacía guardada. */
    private String normalizar(String comentario) {
        return comentario == null || comentario.isBlank() ? null : comentario.trim();
    }
}
