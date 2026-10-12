package com.activehub.usecases.actualizarmiperfil;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios/me")
public class ActualizarMiPerfilController {

    private final ActualizarMiPerfilService actualizarMiPerfilService;

    public ActualizarMiPerfilController(ActualizarMiPerfilService actualizarMiPerfilService) {
        this.actualizarMiPerfilService = actualizarMiPerfilService;
    }

    @PutMapping
    public ActualizarMiPerfilResponse actualizar(
            @Valid @RequestBody ActualizarMiPerfilRequest request, Authentication authentication
    ) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        return actualizarMiPerfilService.actualizar(usuarioId, request);
    }
}
