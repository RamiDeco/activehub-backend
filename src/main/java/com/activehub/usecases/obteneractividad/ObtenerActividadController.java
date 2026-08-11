package com.activehub.usecases.obteneractividad;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/actividades")
public class ObtenerActividadController {

    private final ObtenerActividadService obtenerActividadService;

    public ObtenerActividadController(ObtenerActividadService obtenerActividadService) {
        this.obtenerActividadService = obtenerActividadService;
    }

    @GetMapping("/{id}")
    public ObtenerActividadResponse obtener(@PathVariable UUID id) {
        return obtenerActividadService.obtener(id);
    }
}
