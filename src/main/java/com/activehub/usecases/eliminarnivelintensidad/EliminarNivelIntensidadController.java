package com.activehub.usecases.eliminarnivelintensidad;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/niveles-intensidad")
public class EliminarNivelIntensidadController {

    private final EliminarNivelIntensidadService eliminarNivelIntensidadService;

    public EliminarNivelIntensidadController(EliminarNivelIntensidadService eliminarNivelIntensidadService) {
        this.eliminarNivelIntensidadService = eliminarNivelIntensidadService;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permisos.puede('taxonomia.gestionar')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        eliminarNivelIntensidadService.eliminar(id, actorId);
        return ResponseEntity.noContent().build();
    }
}
