package com.activehub.usecases.aprobarresenia;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/resenas")
public class AprobarReseniaController {

    private final AprobarReseniaService aprobarReseniaService;

    public AprobarReseniaController(AprobarReseniaService aprobarReseniaService) {
        this.aprobarReseniaService = aprobarReseniaService;
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public AprobarReseniaResponse aprobar(@PathVariable UUID id, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return aprobarReseniaService.aprobar(id, actorId);
    }
}
