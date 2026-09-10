package com.activehub.usecases.renovarsesion;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/refresh")
public class RenovarSesionController {

    private final RenovarSesionService renovarSesionService;

    public RenovarSesionController(RenovarSesionService renovarSesionService) {
        this.renovarSesionService = renovarSesionService;
    }

    // Sin @PreAuthorize de rol: cualquier sesión válida puede renovarse. La autenticación
    // (token todavía no vencido) es justamente la condición.
    @PostMapping
    public RenovarSesionResponse renovar(Authentication authentication) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        return renovarSesionService.renovar(usuarioId);
    }
}
