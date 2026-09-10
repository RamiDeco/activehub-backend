package com.activehub.usecases.verimagenactividad;

import com.activehub.domain.actividad.ActividadImagen;
import com.activehub.domain.actividad.ActividadImagenRepository;
import com.activehub.shared.error.NoEncontradoException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerImagenActividadService {

    private final ActividadImagenRepository actividadImagenRepository;
    private final String directorioAlmacenamiento;

    public VerImagenActividadService(
            ActividadImagenRepository actividadImagenRepository,
            @Value("${app.storage.fotos-actividad-dir}") String directorioAlmacenamiento
    ) {
        this.actividadImagenRepository = actividadImagenRepository;
        this.directorioAlmacenamiento = directorioAlmacenamiento;
    }

    @Transactional(readOnly = true)
    public ImagenDescarga ver(UUID imagenId) {
        ActividadImagen imagen = actividadImagenRepository.findById(imagenId)
                .orElseThrow(() -> new NoEncontradoException("Imagen no encontrada."));

        try {
            byte[] contenido = Files.readAllBytes(Path.of(directorioAlmacenamiento).resolve(imagen.getPath()));
            return new ImagenDescarga(tipoContenido(imagen.getPath()), contenido);
        } catch (IOException e) {
            throw new NoEncontradoException("No pudimos leer la imagen.");
        }
    }

    private String tipoContenido(String rutaArchivo) {
        return rutaArchivo.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
    }
}
