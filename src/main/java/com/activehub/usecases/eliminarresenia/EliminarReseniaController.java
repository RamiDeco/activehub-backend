package com.activehub.usecases.eliminarresenia;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumno/resenas")
public class EliminarReseniaController {

    private final EliminarReseniaService eliminarReseniaService;

    public EliminarReseniaController(EliminarReseniaService eliminarReseniaService) {
        this.eliminarReseniaService = eliminarReseniaService;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permisos.puede('resenias.escribir')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, Authentication authentication) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        eliminarReseniaService.eliminar(id, alumnoId);
        return ResponseEntity.noContent().build();
    }
}
