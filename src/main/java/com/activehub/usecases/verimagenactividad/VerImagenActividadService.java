package com.activehub.usecases.verimagenactividad;

import com.activehub.domain.actividad.ActividadImagen;
import com.activehub.domain.actividad.ActividadImagenRepository;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.storage.AlmacenamientoArchivos;
import com.activehub.shared.storage.AlmacenamientoException;
import com.activehub.shared.storage.CarpetaArchivos;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerImagenActividadService {

    private final ActividadImagenRepository actividadImagenRepository;
    private final AlmacenamientoArchivos almacenamiento;

    public VerImagenActividadService(
            ActividadImagenRepository actividadImagenRepository, AlmacenamientoArchivos almacenamiento) {
        this.actividadImagenRepository = actividadImagenRepository;
        this.almacenamiento = almacenamiento;
    }

    @Transactional(readOnly = true)
    public ImagenDescarga ver(UUID imagenId) {
        ActividadImagen imagen = actividadImagenRepository.findById(imagenId)
                .orElseThrow(() -> new NoEncontradoException("Imagen no encontrada."));

        try {
            byte[] contenido = almacenamiento.leer(CarpetaArchivos.FOTOS_ACTIVIDAD, imagen.getPath());
            return new ImagenDescarga(tipoContenido(imagen.getPath()), contenido);
        } catch (AlmacenamientoException e) {
            throw new NoEncontradoException("No pudimos leer la imagen.");
        }
    }

    private String tipoContenido(String rutaArchivo) {
        return rutaArchivo.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
    }
}
