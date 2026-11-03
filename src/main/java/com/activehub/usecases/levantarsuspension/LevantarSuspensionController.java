package com.activehub.usecases.levantarsuspension;

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
@RequestMapping("/api/admin/penalizaciones")
public class LevantarSuspensionController {

    private final LevantarSuspensionService levantarSuspensionService;

    public LevantarSuspensionController(LevantarSuspensionService levantarSuspensionService) {
        this.levantarSuspensionService = levantarSuspensionService;
    }

    @PostMapping("/{id}/levantar")
    @PreAuthorize("@permisos.puede('penalizaciones.gestionar')")
    public LevantarSuspensionResponse levantar(
            @PathVariable UUID id, @Valid @RequestBody LevantarSuspensionRequest request, Authentication authentication) {
        return levantarSuspensionService.levantar(id, request, (UUID) authentication.getPrincipal());
    }
}
