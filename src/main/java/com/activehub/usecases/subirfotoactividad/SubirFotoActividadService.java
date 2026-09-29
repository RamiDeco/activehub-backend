package com.activehub.usecases.subirfotoactividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.storage.AlmacenamientoArchivos;
import com.activehub.shared.storage.AlmacenamientoException;
import com.activehub.shared.storage.CarpetaArchivos;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SubirFotoActividadService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png");

    private final ActividadRepository actividadRepository;
    private final AuditService auditService;
    private final AlmacenamientoArchivos almacenamiento;

    public SubirFotoActividadService(
            ActividadRepository actividadRepository,
            AuditService auditService,
            AlmacenamientoArchivos almacenamiento
    ) {
        this.actividadRepository = actividadRepository;
        this.auditService = auditService;
        this.almacenamiento = almacenamiento;
    }

    @Transactional
    public SubirFotoActividadResponse subir(UUID actividadId, MultipartFile archivo, UUID actorId, boolean puedeModerar) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ValidacionException("Seleccioná una foto para subir.");
        }
        String contentType = archivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new ValidacionException("Formato no permitido. Subí una foto JPG o PNG.");
        }

        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));

        if (!puedeModerar && !actividad.getInstructor().getId().equals(actorId)) {
            throw new SinPermisoException("No podés subir una foto a una actividad que no te pertenece.");
        }

        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "foto";
        String nombreGuardado = UUID.randomUUID() + extension(nombreOriginal);

        try {
            almacenamiento.guardar(CarpetaArchivos.FOTOS_ACTIVIDAD, nombreGuardado, archivo.getBytes(), contentType);
        } catch (java.io.IOException | AlmacenamientoException e) {
            throw new ValidacionException("No pudimos guardar la foto. Intentá de nuevo.");
        }

        String fotoAnterior = actividad.getFotoPath();
        actividad.setFotoPath(nombreGuardado);
        actividadRepository.save(actividad);

        if (fotoAnterior != null) {
            almacenamiento.borrarSiExiste(CarpetaArchivos.FOTOS_ACTIVIDAD, fotoAnterior);
        }

        auditService.registrar(actorId, AuditAccion.FOTO_ACTIVIDAD_SUBIDA, "Actividad", actividad.getId(), null);

        return new SubirFotoActividadResponse(actividad.getId());
    }

    private String extension(String nombreOriginal) {
        int i = nombreOriginal.lastIndexOf('.');
        return i >= 0 ? nombreOriginal.substring(i) : "";
    }
}
