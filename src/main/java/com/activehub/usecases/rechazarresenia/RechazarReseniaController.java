package com.activehub.usecases.rechazarresenia;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/resenas")
public class RechazarReseniaController {

    private final RechazarReseniaService rechazarReseniaService;

    public RechazarReseniaController(RechazarReseniaService rechazarReseniaService) {
        this.rechazarReseniaService = rechazarReseniaService;
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("@permisos.puede('denuncias.resolver')")
    public RechazarReseniaResponse rechazar(@PathVariable UUID id, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return rechazarReseniaService.rechazar(id, actorId);
    }
}
