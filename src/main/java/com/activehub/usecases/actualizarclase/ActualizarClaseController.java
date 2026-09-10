package com.activehub.usecases.actualizarclase;

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
@RequestMapping("/api/instructor/clases")
public class ActualizarClaseController {

    private final ActualizarClaseService actualizarClaseService;

    public ActualizarClaseController(ActualizarClaseService actualizarClaseService) {
        this.actualizarClaseService = actualizarClaseService;
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permisos.puede('clases.gestionar')")
    public ActualizarClaseResponse actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarClaseRequest request,
            Authentication authentication) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        return actualizarClaseService.actualizar(id, request, instructorId);
    }
}
