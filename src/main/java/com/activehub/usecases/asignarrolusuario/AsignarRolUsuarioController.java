package com.activehub.usecases.asignarrolusuario;

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
public class AsignarRolUsuarioController {

    private final AsignarRolUsuarioService asignarRolUsuarioService;

    public AsignarRolUsuarioController(AsignarRolUsuarioService asignarRolUsuarioService) {
        this.asignarRolUsuarioService = asignarRolUsuarioService;
    }

    /** Cambiar el rol es tocar quien puede que cosa: va con el permiso de configuracion de roles. */
    @PutMapping("/{id}/rol")
    @PreAuthorize("@permisos.puede('roles.configurar')")
    public AsignarRolUsuarioResponse asignar(
            @PathVariable UUID id,
            @Valid @RequestBody AsignarRolUsuarioRequest request,
            Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return asignarRolUsuarioService.asignar(id, request, actorId);
    }
}
