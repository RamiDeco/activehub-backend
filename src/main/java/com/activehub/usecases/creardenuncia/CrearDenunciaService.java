package com.activehub.usecases.creardenuncia;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearDenunciaService {

    private static final Duration UMBRAL_DENUNCIA = Duration.ofHours(1);

    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final DenunciaRepository denunciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;
    private final NotificacionService notificacionService;
    private final Clock clock;

    public CrearDenunciaService(
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            DenunciaRepository denunciaRepository,
            UsuarioRepository usuarioRepository,
            AuditService auditService,
            NotificacionService notificacionService,
            Clock clock
    ) {
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.denunciaRepository = denunciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
        this.notificacionService = notificacionService;
        this.clock = clock;
    }

    @Transactional
    public CrearDenunciaResponse crear(UUID claseId, CrearDenunciaRequest request, UUID alumnoId) {
        Clase clase = claseRepository.findById(claseId)
                .orElseThrow(() -> new NoEncontradoException("Clase no encontrada."));

        if (!inscripcionRepository.existsByClaseIdAndAlumnoIdAndEstado(claseId, alumnoId, EstadoInscripcion.INSCRIPTO)) {
            throw new ValidacionException("Solo podés denunciar clases en las que estuviste inscripto.");
        }
        if (Duration.between(clase.getFechaHora(), clock.instant()).compareTo(UMBRAL_DENUNCIA) < 0) {
            throw new ValidacionException("Todavía no pasó 1 hora desde el inicio de la clase.");
        }
        if (denunciaRepository.existsByClaseIdAndAlumnoId(claseId, alumnoId)) {
            throw new ValidacionException("Ya denunciaste esta clase.");
        }

        Denuncia denuncia = new Denuncia();
        denuncia.setClase(clase);
        denuncia.setAlumno(usuarioRepository.getReferenceById(alumnoId));
        denuncia.setMotivo(request.motivo());
        denuncia = denunciaRepository.saveAndFlush(denuncia);

        notificacionService.notificar(
                clase.getActividad().getInstructor().getId(),
                TipoNotificacion.DENUNCIA_RECIBIDA,
                "Recibiste una denuncia por inasistencia a la clase de \"" + clase.getActividad().getNombre()
                        + "\" del " + NotificacionMensajes.formatFechaHora(clase.getFechaHora()) + ".",
                denuncia.getId());

        auditService.registrar(alumnoId, AuditAccion.DENUNCIA_CREADA, "Denuncia", denuncia.getId(), null);

        return new CrearDenunciaResponse(
                denuncia.getId(), claseId, alumnoId, denuncia.getMotivo(), denuncia.getEstado().getEtiqueta(), denuncia.getCreatedAt());
    }
}
