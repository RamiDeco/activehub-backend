package com.activehub.usecases.actualizarcategoria;

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
@RequestMapping("/api/admin/categorias")
public class ActualizarCategoriaController {

    private final ActualizarCategoriaService actualizarCategoriaService;

    public ActualizarCategoriaController(ActualizarCategoriaService actualizarCategoriaService) {
        this.actualizarCategoriaService = actualizarCategoriaService;
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permisos.puede('taxonomia.gestionar')")
    public ActualizarCategoriaResponse actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarCategoriaRequest request,
            Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return actualizarCategoriaService.actualizar(id, request, actorId);
    }
}
