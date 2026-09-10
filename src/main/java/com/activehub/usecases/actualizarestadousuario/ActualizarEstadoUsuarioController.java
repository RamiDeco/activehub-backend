package com.activehub.usecases.actualizarestadousuario;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/usuarios")
public class ActualizarEstadoUsuarioController {

    private final ActualizarEstadoUsuarioService actualizarEstadoUsuarioService;

    public ActualizarEstadoUsuarioController(ActualizarEstadoUsuarioService actualizarEstadoUsuarioService) {
        this.actualizarEstadoUsuarioService = actualizarEstadoUsuarioService;
    }

    @PostMapping("/{id}/estado")
    @PreAuthorize("@permisos.puede('usuarios.gestionar')")
    public ActualizarEstadoUsuarioResponse actualizar(
            @PathVariable UUID id, @Valid @RequestBody ActualizarEstadoUsuarioRequest request, Authentication authentication
    ) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return actualizarEstadoUsuarioService.actualizar(id, request, actorId);
    }
}
