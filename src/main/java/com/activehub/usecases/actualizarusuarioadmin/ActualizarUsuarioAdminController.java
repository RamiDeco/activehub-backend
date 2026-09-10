package com.activehub.usecases.actualizarusuarioadmin;

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
@RequestMapping("/api/admin/usuarios")
public class ActualizarUsuarioAdminController {

    private final ActualizarUsuarioAdminService actualizarUsuarioAdminService;

    public ActualizarUsuarioAdminController(ActualizarUsuarioAdminService actualizarUsuarioAdminService) {
        this.actualizarUsuarioAdminService = actualizarUsuarioAdminService;
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permisos.puede('usuarios.gestionar')")
    public ActualizarUsuarioAdminResponse actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarUsuarioAdminRequest request,
            Authentication authentication
    ) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return actualizarUsuarioAdminService.actualizar(id, request, actorId);
    }
}
