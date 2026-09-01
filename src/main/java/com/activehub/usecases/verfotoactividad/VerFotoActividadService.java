package com.activehub.usecases.verfotoactividad;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.shared.error.NoEncontradoException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerFotoActividadService {

    private final ActividadRepository actividadRepository;
    private final String directorioAlmacenamiento;

    public VerFotoActividadService(
            ActividadRepository actividadRepository,
            @Value("${app.storage.fotos-actividad-dir}") String directorioAlmacenamiento
    ) {
        this.actividadRepository = actividadRepository;
        this.directorioAlmacenamiento = directorioAlmacenamiento;
    }

    @Transactional(readOnly = true)
    public FotoDescarga ver(UUID actividadId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));

        if (actividad.getFotoPath() == null) {
            throw new NoEncontradoException("La actividad no tiene foto.");
        }

        try {
            byte[] contenido = Files.readAllBytes(Path.of(directorioAlmacenamiento).resolve(actividad.getFotoPath()));
            return new FotoDescarga(tipoContenido(actividad.getFotoPath()), contenido);
        } catch (IOException e) {
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
