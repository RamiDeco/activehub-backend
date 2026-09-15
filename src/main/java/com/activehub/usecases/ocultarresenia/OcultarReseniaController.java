package com.activehub.usecases.ocultarresenia;

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
@RequestMapping("/api/admin/resenas")
public class OcultarReseniaController {

    private final OcultarReseniaService ocultarReseniaService;

    public OcultarReseniaController(OcultarReseniaService ocultarReseniaService) {
        this.ocultarReseniaService = ocultarReseniaService;
    }

    /** Mismo permiso que moderar y que resolver denuncias: es la misma responsabilidad. */
    @PostMapping("/{id}/ocultar")
    @PreAuthorize("@permisos.puede('denuncias.resolver')")
    public OcultarReseniaResponse ocultar(
            @PathVariable UUID id,
            @Valid @RequestBody OcultarReseniaRequest request,
            Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return ocultarReseniaService.ocultar(id, request, actorId);
    }
}
