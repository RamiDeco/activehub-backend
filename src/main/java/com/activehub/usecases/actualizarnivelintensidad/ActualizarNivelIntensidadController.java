package com.activehub.usecases.actualizarnivelintensidad;

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
@RequestMapping("/api/admin/niveles-intensidad")
public class ActualizarNivelIntensidadController {

    private final ActualizarNivelIntensidadService actualizarNivelIntensidadService;

    public ActualizarNivelIntensidadController(
            ActualizarNivelIntensidadService actualizarNivelIntensidadService) {
        this.actualizarNivelIntensidadService = actualizarNivelIntensidadService;
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permisos.puede('taxonomia.gestionar')")
    public ActualizarNivelIntensidadResponse actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarNivelIntensidadRequest request,
            Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return actualizarNivelIntensidadService.actualizar(id, request, actorId);
    }
}
