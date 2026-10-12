package com.activehub.usecases.actualizaractividad;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/actividades")
public class ActualizarActividadController {

    private final ActualizarActividadService actualizarActividadService;

    public ActualizarActividadController(ActualizarActividadService actualizarActividadService) {
        this.actualizarActividadService = actualizarActividadService;
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permisos.puede('actividades.publicar')")
    public ActualizarActividadResponse actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarActividadRequest request,
            Authentication authentication) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        return actualizarActividadService.actualizar(id, request, instructorId);
    }
}
