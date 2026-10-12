package com.activehub.usecases.actualizarpermisosrol;

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
@RequestMapping("/api/admin/roles")
public class ActualizarPermisosRolController {

    private final ActualizarPermisosRolService actualizarPermisosRolService;

    public ActualizarPermisosRolController(ActualizarPermisosRolService actualizarPermisosRolService) {
        this.actualizarPermisosRolService = actualizarPermisosRolService;
    }

    @PutMapping("/{id}/permisos")
    @PreAuthorize("@permisos.puede('roles.configurar')")
    public ActualizarPermisosRolResponse actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarPermisosRolRequest request,
            Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return actualizarPermisosRolService.actualizar(id, request, actorId);
    }
}
