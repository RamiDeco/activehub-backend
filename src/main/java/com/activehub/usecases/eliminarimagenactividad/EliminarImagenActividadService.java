package com.activehub.usecases.eliminarimagenactividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadImagen;
import com.activehub.domain.actividad.ActividadImagenRepository;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarImagenActividadService {

    private final ActividadRepository actividadRepository;
    private final ActividadImagenRepository actividadImagenRepository;
    private final AuditService auditService;
    private final String directorioAlmacenamiento;

    public EliminarImagenActividadService(
            ActividadRepository actividadRepository,
            ActividadImagenRepository actividadImagenRepository,
            AuditService auditService,
            @Value("${app.storage.fotos-actividad-dir}") String directorioAlmacenamiento
    ) {
        this.actividadRepository = actividadRepository;
        this.actividadImagenRepository = actividadImagenRepository;
        this.auditService = auditService;
        this.directorioAlmacenamiento = directorioAlmacenamiento;
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

        try {
            Files.deleteIfExists(Path.of(directorioAlmacenamiento).resolve(imagen.getPath()));
        } catch (IOException e) {
            // La fila ya no está; un archivo huérfano no justifica fallar la operación.
        }

        auditService.registrar(
                actorId, AuditAccion.IMAGEN_ACTIVIDAD_ELIMINADA, "Actividad", actividadId, imagenId.toString());
    }
}
