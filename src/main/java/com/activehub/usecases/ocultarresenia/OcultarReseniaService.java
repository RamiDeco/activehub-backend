package com.activehub.usecases.ocultarresenia;

import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El administrador baja una reseña <b>ya publicada</b>, sin depender de que alguien la denuncie.
 *
 * <p><b>El agujero que cierra.</b> Sin esto una reseña publicada solo se podria sacar por un
 * camino: que el instructor duenio de la actividad la denunciara ({@code denunciarresenia}) y
 * que el admin resolviera esa denuncia con {@code OCULTAR_RESENIA}. O sea que si se publica un
 * insulto, el admin que lo ve no tendria ningun boton: dependeria de que el instructor
 * estuviera mirando y la reportara. Peor, el instructor solo puede denunciar una vez la misma
 * resenia ({@code existsByReseniaIdAndDenuncianteId}), asi que un caso desestimado por error
 * quedaria cerrado para siempre. Desde V33 las resenias se publican al crearse, sin
 * moderacion previa, asi que este es el control posterior del administrador.
 *
 * <p><b>Oculta, no borra — y es a proposito.</b> La fila queda entera en la base con su autor,
 * su texto y su fecha. Si el contenido llega a ser algo en lo que deba intervenir la justicia
 * (una amenaza, una difamacion), la evidencia y la identidad de quien lo escribio tienen que
 * seguir existiendo; borrarla destruiria justamente lo que haria falta. Lo que cambia es que
 * {@code findVisiblesPorActividad} y {@code recalcularRating} la excluyen: desaparece del
 * detalle publico y deja de contar en las estrellas.
 *
 * <p><b>El motivo es obligatorio.</b> Ocultar contenido de otro es una decision que hay que
 * poder justificar despues: queda en auditoria junto con quien la tomo y cuando.
 */
@Service
public class OcultarReseniaService {

    private final ReseniaRepository reseniaRepository;
    private final ActividadRepository actividadRepository;
    private final AuditService auditService;
    private final NotificacionService notificacionService;

    public OcultarReseniaService(
            ReseniaRepository reseniaRepository,
            ActividadRepository actividadRepository,
            AuditService auditService,
            NotificacionService notificacionService
    ) {
        this.reseniaRepository = reseniaRepository;
        this.actividadRepository = actividadRepository;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public OcultarReseniaResponse ocultar(UUID id, OcultarReseniaRequest request, UUID actorId) {
        Resenia resenia = reseniaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Reseña no encontrada."));

        if (resenia.isOculta()) {
            throw new ValidacionException("Esta reseña ya está oculta.");
        }
        // Todo lo que haga falta de la entidad se lee ANTES de recalcularRating, que es un
        // @Modifying con clearAutomatically y deja la entidad detached: una relacion LAZY leida
        // despues explota con LazyInitializationException.
        UUID actividadId = resenia.getClase().getActividad().getId();
        UUID alumnoId = resenia.getAlumno().getId();
        String contexto = "la clase de \"" + resenia.getClase().getActividad().getNombre()
                + "\" del " + NotificacionMensajes.formatFechaHora(resenia.getClase().getFechaHora());
        String motivo = request.motivo().trim();

        resenia.setOculta(true);
        reseniaRepository.save(resenia);
        actividadRepository.recalcularRating(actividadId);

        notificacionService.notificar(
                alumnoId,
                TipoNotificacion.RESENIA_RECHAZADA,
                "Un administrador ocultó tu reseña sobre " + contexto + ". Motivo: " + motivo,
                id, Destino.resenia(id));

        auditService.registrar(actorId, AuditAccion.RESENIA_OCULTADA, "Resenia", id, motivo);

        return new OcultarReseniaResponse(id, true);
    }
}
