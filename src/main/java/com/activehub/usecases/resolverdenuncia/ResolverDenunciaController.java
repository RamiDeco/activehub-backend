package com.activehub.usecases.resolverdenuncia;

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
@RequestMapping("/api/admin/denuncias")
public class ResolverDenunciaController {

    private final ResolverDenunciaService resolverDenunciaService;

    public ResolverDenunciaController(ResolverDenunciaService resolverDenunciaService) {
        this.resolverDenunciaService = resolverDenunciaService;
    }

    @PostMapping("/{id}/resolver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResolverDenunciaResponse resolver(
            @PathVariable UUID id, @Valid @RequestBody ResolverDenunciaRequest request, Authentication authentication
    ) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return resolverDenunciaService.resolver(id, request, actorId);
    }
}
