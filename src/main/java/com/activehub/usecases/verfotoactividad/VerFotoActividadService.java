package com.activehub.usecases.verfotoactividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.storage.AlmacenamientoArchivos;
import com.activehub.shared.storage.AlmacenamientoException;
import com.activehub.shared.storage.CarpetaArchivos;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerFotoActividadService {

    private final ActividadRepository actividadRepository;
    private final AlmacenamientoArchivos almacenamiento;

    public VerFotoActividadService(ActividadRepository actividadRepository, AlmacenamientoArchivos almacenamiento) {
        this.actividadRepository = actividadRepository;
        this.almacenamiento = almacenamiento;
    }

    @Transactional(readOnly = true)
    public FotoDescarga ver(UUID actividadId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));

        if (actividad.getFotoPath() == null) {
            throw new NoEncontradoException("La actividad no tiene foto.");
        }

        try {
            byte[] contenido = almacenamiento.leer(CarpetaArchivos.FOTOS_ACTIVIDAD, actividad.getFotoPath());
            return new FotoDescarga(tipoContenido(actividad.getFotoPath()), contenido);
        } catch (AlmacenamientoException e) {
            throw new NoEncontradoException("No pudimos leer la foto.");
        }
    }

    private String tipoContenido(String rutaArchivo) {
        String ruta = rutaArchivo.toLowerCase();
        if (ruta.endsWith(".png")) {
            return "image/png";
        }
        return "image/jpeg";
    }
}
