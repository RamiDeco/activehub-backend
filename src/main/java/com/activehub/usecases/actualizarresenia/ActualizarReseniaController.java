package com.activehub.usecases.actualizarresenia;

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
@RequestMapping("/api/alumno/resenas")
public class ActualizarReseniaController {

    private final ActualizarReseniaService actualizarReseniaService;

    public ActualizarReseniaController(ActualizarReseniaService actualizarReseniaService) {
        this.actualizarReseniaService = actualizarReseniaService;
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permisos.puede('resenias.escribir')")
    public ActualizarReseniaResponse actualizar(
            @PathVariable UUID id, @Valid @RequestBody ActualizarReseniaRequest request, Authentication authentication
    ) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        return actualizarReseniaService.actualizar(id, request, alumnoId);
    }
}
