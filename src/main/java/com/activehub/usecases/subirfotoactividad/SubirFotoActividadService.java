package com.activehub.usecases.subirfotoactividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SubirFotoActividadService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png");

    private final ActividadRepository actividadRepository;
    private final AuditService auditService;
    private final String directorioAlmacenamiento;

    public SubirFotoActividadService(
            ActividadRepository actividadRepository,
            AuditService auditService,
            @Value("${app.storage.fotos-actividad-dir}") String directorioAlmacenamiento
    ) {
        this.actividadRepository = actividadRepository;
        this.auditService = auditService;
        this.directorioAlmacenamiento = directorioAlmacenamiento;
    }

    @Transactional
    public SubirFotoActividadResponse subir(UUID actividadId, MultipartFile archivo, UUID actorId, boolean esAdmin) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ValidacionException("Seleccioná una foto para subir.");
        }
        String contentType = archivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new ValidacionException("Formato no permitido. Subí una foto JPG o PNG.");
        }

        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));

        if (!esAdmin && !actividad.getInstructor().getId().equals(actorId)) {
            throw new SinPermisoException("No podés subir una foto a una actividad que no te pertenece.");
        }

        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "foto";
        String nombreEnDisco = UUID.randomUUID() + extension(nombreOriginal);

        try {
            Path directorio = Path.of(directorioAlmacenamiento);
            Files.createDirectories(directorio);
            Files.write(directorio.resolve(nombreEnDisco), archivo.getBytes());
        } catch (IOException e) {
            throw new ValidacionException("No pudimos guardar la foto. Intentá de nuevo.");
        }

        String fotoAnterior = actividad.getFotoPath();
        actividad.setFotoPath(nombreEnDisco);
        actividadRepository.save(actividad);

        if (fotoAnterior != null) {
            try {
                Files.deleteIfExists(Path.of(directorioAlmacenamiento).resolve(fotoAnterior));
            } catch (IOException e) {
                // no bloquea la respuesta si no se pudo borrar el archivo viejo
            }
        }

        auditService.registrar(actorId, AuditAccion.FOTO_ACTIVIDAD_SUBIDA, "Actividad", actividad.getId(), null);

        return new SubirFotoActividadResponse(actividad.getId());
    }

    private String extension(String nombreOriginal) {
        int i = nombreOriginal.lastIndexOf('.');
        return i >= 0 ? nombreOriginal.substring(i) : "";
    }
}
