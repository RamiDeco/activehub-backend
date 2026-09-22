package com.activehub.usecases.agregarimagenactividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadImagen;
import com.activehub.domain.actividad.ActividadImagenRepository;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Galería de la actividad (sección 2 pide imagenes[], en plural).
 *
 * <p>Convive con {@code subirfotoactividad}, que sigue manejando la <b>portada</b>
 * ({@code actividad.fotoPath}) y es la que se ve en las tarjetas del catálogo. La primera
 * imagen que se sube cuando todavía no hay portada pasa a serlo, para que no haya que subir
 * la misma foto dos veces.
 */
@Service
public class AgregarImagenActividadService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png");
    static final int MAXIMO_POR_ACTIVIDAD = 6;

    private final ActividadRepository actividadRepository;
    private final ActividadImagenRepository actividadImagenRepository;
    private final AuditService auditService;
    private final AlmacenamientoArchivos almacenamiento;

    public AgregarImagenActividadService(
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
    public AgregarImagenActividadResponse agregar(
            UUID actividadId, MultipartFile archivo, UUID actorId, boolean puedeModerar) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ValidacionException("Seleccioná una imagen para subir.");
        }
        String contentType = archivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new ValidacionException("Formato no permitido. Subí una imagen JPG o PNG.");
        }

        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));

        if (!puedeModerar && !actividad.getInstructor().getId().equals(actorId)) {
            throw new SinPermisoException("No podés subir imágenes a una actividad que no te pertenece.");
        }

        int existentes = actividadImagenRepository.countByActividadId(actividadId);
        if (existentes >= MAXIMO_POR_ACTIVIDAD) {
            throw new ValidacionException(
                    "Llegaste al máximo de " + MAXIMO_POR_ACTIVIDAD + " imágenes. Borrá alguna para subir otra.");
        }

        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "imagen";
        String nombreGuardado = UUID.randomUUID() + extension(nombreOriginal);

        try {
            almacenamiento.guardar(CarpetaArchivos.FOTOS_ACTIVIDAD, nombreGuardado, archivo.getBytes(), contentType);
        } catch (java.io.IOException | AlmacenamientoException e) {
            throw new ValidacionException("No pudimos guardar la imagen. Intentá de nuevo.");
        }
        // El almacenamiento no es transaccional, ni el disco ni Supabase: si la transacción
        // termina en rollback hay que borrar a mano el archivo ya escrito, o queda basura
        // huérfana. Mismo patrón que registrarinstructor.
        registrarBorradoSiHayRollback(nombreGuardado);

        ActividadImagen imagen = new ActividadImagen();
        imagen.setActividad(actividad);
        imagen.setPath(nombreGuardado);
        imagen.setOrden(existentes);
        imagen = actividadImagenRepository.saveAndFlush(imagen);

        if (actividad.getFotoPath() == null) {
            actividad.setFotoPath(nombreGuardado);
            actividadRepository.save(actividad);
        }

        auditService.registrar(
                actorId, AuditAccion.IMAGEN_ACTIVIDAD_AGREGADA, "Actividad", actividadId, imagen.getId().toString());

        return new AgregarImagenActividadResponse(imagen.getId(), actividadId, imagen.getOrden());
    }

    private void registrarBorradoSiHayRollback(String nombreGuardado) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    almacenamiento.borrarSiExiste(CarpetaArchivos.FOTOS_ACTIVIDAD, nombreGuardado);
                }
            }
        });
    }

    private String extension(String nombreOriginal) {
        int i = nombreOriginal.lastIndexOf('.');
        return i >= 0 ? nombreOriginal.substring(i) : "";
    }
}
