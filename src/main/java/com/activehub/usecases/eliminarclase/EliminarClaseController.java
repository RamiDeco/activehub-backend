package com.activehub.usecases.eliminarclase;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/clases")
public class EliminarClaseController {

    private final EliminarClaseService eliminarClaseService;

    public EliminarClaseController(EliminarClaseService eliminarClaseService) {
        this.eliminarClaseService = eliminarClaseService;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, Authentication authentication) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        eliminarClaseService.eliminar(id, instructorId);
        return ResponseEntity.noContent().build();
    }
}
