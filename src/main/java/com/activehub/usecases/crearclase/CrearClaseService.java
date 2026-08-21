package com.activehub.usecases.crearclase;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.favorito.FavoritoRepository;
import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearClaseService {

    private final ActividadRepository actividadRepository;
    private final ClaseRepository claseRepository;
    private final PerfilInstructorRepository perfilInstructorRepository;
    private final FavoritoRepository favoritoRepository;
    private final NotificacionService notificacionService;
    private final AuditService auditService;

    public CrearClaseService(
            ActividadRepository actividadRepository,
            ClaseRepository claseRepository,
            PerfilInstructorRepository perfilInstructorRepository,
            FavoritoRepository favoritoRepository,
            NotificacionService notificacionService,
            AuditService auditService
    ) {
        this.actividadRepository = actividadRepository;
        this.claseRepository = claseRepository;
        this.perfilInstructorRepository = perfilInstructorRepository;
        this.favoritoRepository = favoritoRepository;
        this.notificacionService = notificacionService;
        this.auditService = auditService;
    }

    @Transactional
    public CrearClaseResponse crear(UUID actividadId, CrearClaseRequest request, UUID instructorId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));

        if (!actividad.getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés crear clases para una actividad que no te pertenece.");
        }

        boolean aprobado = perfilInstructorRepository.findByUsuarioId(instructorId)
                .map(p -> p.getEstadoVerificacion() == EstadoVerificacion.APROBADO)
                .orElse(false);
        if (!aprobado) {
            throw new SinPermisoException(
                    "Tu perfil de instructor todavía no fue aprobado. No podés crear clases hasta que un administrador lo valide.");
        }

        Clase clase = new Clase();
        clase.setActividad(actividad);
        clase.setFechaHora(request.fechaHora());
        clase.setCuposMax(request.cuposMax() != null ? request.cuposMax() : actividad.getCuposMax());
        clase.setEstado(EstadoClase.Programada);
        clase.setCuposOcupados(0);
        clase = claseRepository.save(clase);

        auditService.registrar(instructorId, AuditAccion.CLASE_CREADA, "Clase", clase.getId(), null);

        String mensaje = "Se agregó un nuevo horario para \"" + actividad.getNombre()
                + "\" (uno de tus favoritos): " + NotificacionMensajes.formatFechaHora(clase.getFechaHora()) + ".";
        for (var favorito : favoritoRepository.findByActividadId(actividadId)) {
            notificacionService.notificar(
                    favorito.getUsuario().getId(), TipoNotificacion.NUEVO_HORARIO_FAVORITO, mensaje, clase.getId());
        }

        return new CrearClaseResponse(
                clase.getId(), actividad.getId(), clase.getFechaHora(), clase.getEstado().name(),
                clase.getCuposMax(), clase.getCuposOcupados());
    }
}
