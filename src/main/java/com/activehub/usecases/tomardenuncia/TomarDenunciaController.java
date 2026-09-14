package com.activehub.usecases.tomardenuncia;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/denuncias")
public class TomarDenunciaController {

    private final TomarDenunciaService tomarDenunciaService;

    public TomarDenunciaController(TomarDenunciaService tomarDenunciaService) {
        this.tomarDenunciaService = tomarDenunciaService;
    }

    @PostMapping("/{id}/auditar")
    @PreAuthorize("@permisos.puede('denuncias.resolver')")
    public TomarDenunciaResponse tomar(@PathVariable UUID id, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return tomarDenunciaService.tomar(id, actorId);
    }
}
