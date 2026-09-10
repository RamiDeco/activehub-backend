package com.activehub.usecases.verimagenactividad;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fotos/actividad/imagen")
public class VerImagenActividadController {

    private final VerImagenActividadService verImagenActividadService;

    public VerImagenActividadController(VerImagenActividadService verImagenActividadService) {
        this.verImagenActividadService = verImagenActividadService;
    }

    // Público como el resto del catálogo: la imagen se sirve por su id, que solo se conoce
    // a través del detalle de la actividad.
    @GetMapping("/{imagenId}")
    public ResponseEntity<byte[]> ver(@PathVariable UUID imagenId) {
        ImagenDescarga imagen = verImagenActividadService.ver(imagenId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.tipoContenido()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(imagen.contenido());
    }
}
