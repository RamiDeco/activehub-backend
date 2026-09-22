package com.activehub.usecases.eliminarimagenactividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadImagen;
import com.activehub.domain.actividad.ActividadImagenRepository;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.storage.AlmacenamientoArchivos;
import com.activehub.shared.storage.CarpetaArchivos;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarImagenActividadService {

    private final ActividadRepository actividadRepository;
    private final ActividadImagenRepository actividadImagenRepository;
    private final AuditService auditService;
    private final AlmacenamientoArchivos almacenamiento;

    public EliminarImagenActividadService(
            ActividadRepository actividadRepository,
            ActividadImagenRepository actividadImagenRepository,
            AuditService auditService,
            AlmacenamientoArchivos almacenamiento
    ) {
        this.actividadRepository = actividadRepository;
        this.actividadImagenRepository = actividadImagenRepository;
        this.auditService = auditService;
        this.almacenamiento = almacenamiento;
    }

    @Transactional
    public void eliminar(UUID actividadId, UUID imagenId, UUID actorId, boolean puedeModerar) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));

        if (!puedeModerar && !actividad.getInstructor().getId().equals(actorId)) {
            throw new SinPermisoException("No podés borrar imágenes de una actividad que no te pertenece.");
        }

        ActividadImagen imagen = actividadImagenRepository.findById(imagenId)
                .orElseThrow(() -> new NoEncontradoException("Imagen no encontrada."));
        if (!imagen.getActividad().getId().equals(actividadId)) {
            throw new NoEncontradoException("Imagen no encontrada.");
        }

        actividadImagenRepository.delete(imagen);

        // Si la que se borra era la portada, promover la primera que quede en la galería;
        // si no queda ninguna, la actividad se queda sin foto (el frontend cae al photoTint).
        if (imagen.getPath().equals(actividad.getFotoPath())) {
            List<ActividadImagen> restantes = actividadImagenRepository
                    .findByActividadIdOrderByOrdenAsc(actividadId).stream()
                    .filter(i -> !i.getId().equals(imagenId))
                    .toList();
            actividad.setFotoPath(restantes.isEmpty() ? null : restantes.get(0).getPath());
            actividadRepository.save(actividad);
        }

        // La fila ya no está; un archivo huérfano no justifica fallar la operación, y por eso
        // `borrarSiExiste` no lanza.
        almacenamiento.borrarSiExiste(CarpetaArchivos.FOTOS_ACTIVIDAD, imagen.getPath());

        auditService.registrar(
                actorId, AuditAccion.IMAGEN_ACTIVIDAD_ELIMINADA, "Actividad", actividadId, imagenId.toString());
    }
}
