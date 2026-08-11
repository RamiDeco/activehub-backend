package com.activehub.usecases.cancelarinscripcion;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumno/inscripciones")
public class CancelarInscripcionController {

    private final CancelarInscripcionService cancelarInscripcionService;

    public CancelarInscripcionController(CancelarInscripcionService cancelarInscripcionService) {
        this.cancelarInscripcionService = cancelarInscripcionService;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id, Authentication authentication) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        cancelarInscripcionService.cancelar(id, alumnoId);
        return ResponseEntity.noContent().build();
    }
}
