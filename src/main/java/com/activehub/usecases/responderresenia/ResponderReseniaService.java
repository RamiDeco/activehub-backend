package com.activehub.usecases.responderresenia;

import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E2I-HU11 criterio 4: el instructor responde públicamente una reseña sobre su actividad.
 *
 * <p>El botón "Enviar respuesta" existía en la pantalla pero no tenía dónde guardar el texto
 * ({@code Resenia} no tenía el campo), así que la respuesta se perdía apenas se recargaba.
 */
@Service
public class ResponderReseniaService {

    private final ReseniaRepository reseniaRepository;
    private final InstructorVerificadoGuard instructorVerificadoGuard;
    private final NotificacionService notificacionService;
    private final AuditService auditService;
    private final Clock clock;

    public ResponderReseniaService(
            ReseniaRepository reseniaRepository,
            InstructorVerificadoGuard instructorVerificadoGuard,
            NotificacionService notificacionService,
            AuditService auditService,
            Clock clock
    ) {
        this.reseniaRepository = reseniaRepository;
        this.instructorVerificadoGuard = instructorVerificadoGuard;
        this.notificacionService = notificacionService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public ResponderReseniaResponse responder(UUID reseniaId, ResponderReseniaRequest request, UUID instructorId) {
        instructorVerificadoGuard.exigirVerificado(instructorId, "responder reseñas");

        Resenia resenia = reseniaRepository.findById(reseniaId)
                .orElseThrow(() -> new NoEncontradoException("Reseña no encontrada."));

        if (!resenia.getClase().getActividad().getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés responder una reseña de otro instructor.");
        }

        // Responder una reseña que el admin todavía no aprobó publicaría la respuesta antes
        // que el comentario que contesta.
        if (resenia.isEnModeracion()) {
            throw new ValidacionException("Todavía no podés responder: la reseña está pendiente de moderación.");
        }
        if (resenia.isOculta()) {
            throw new ValidacionException("Esta reseña fue ocultada y ya no se puede responder.");
        }

        resenia.setRespuestaInstructor(request.respuesta().trim());
        resenia.setRespuestaInstructorAt(clock.instant());
        reseniaRepository.save(resenia);

        notificacionService.notificar(
                resenia.getAlumno().getId(),
                TipoNotificacion.RESENIA_RESPONDIDA,
                "El instructor respondió tu reseña sobre \"" + resenia.getClase().getActividad().getNombre() + "\".",
                resenia.getId(), Destino.resenia(resenia.getId()));

        auditService.registrar(instructorId, AuditAccion.RESENIA_RESPONDIDA, "Resenia", reseniaId, null);

        return new ResponderReseniaResponse(
                resenia.getId(), resenia.getRespuestaInstructor(), resenia.getRespuestaInstructorAt());
    }
}
