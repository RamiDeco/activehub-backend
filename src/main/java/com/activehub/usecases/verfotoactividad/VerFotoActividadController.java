package com.activehub.usecases.verfotoactividad;

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
@RequestMapping("/api/fotos/actividad")
public class VerFotoActividadController {

    private final VerFotoActividadService verFotoActividadService;

    public VerFotoActividadController(VerFotoActividadService verFotoActividadService) {
        this.verFotoActividadService = verFotoActividadService;
    }

    @GetMapping("/{actividadId}")
    public ResponseEntity<byte[]> ver(@PathVariable UUID actividadId) {
        FotoDescarga foto = verFotoActividadService.ver(actividadId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipoContenido()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(foto.contenido());
    }
}
