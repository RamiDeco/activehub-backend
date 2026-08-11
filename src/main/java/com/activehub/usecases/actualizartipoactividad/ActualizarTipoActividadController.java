package com.activehub.usecases.actualizartipoactividad;

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
@RequestMapping("/api/admin/tipos-actividad")
public class ActualizarTipoActividadController {

    private final ActualizarTipoActividadService actualizarTipoActividadService;

    public ActualizarTipoActividadController(ActualizarTipoActividadService actualizarTipoActividadService) {
        this.actualizarTipoActividadService = actualizarTipoActividadService;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ActualizarTipoActividadResponse actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarTipoActividadRequest request,
            Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return actualizarTipoActividadService.actualizar(id, request, actorId);
    }
}
