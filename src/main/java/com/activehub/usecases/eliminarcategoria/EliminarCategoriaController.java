package com.activehub.usecases.eliminarcategoria;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categorias")
public class EliminarCategoriaController {

    private final EliminarCategoriaService eliminarCategoriaService;

    public EliminarCategoriaController(EliminarCategoriaService eliminarCategoriaService) {
        this.eliminarCategoriaService = eliminarCategoriaService;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permisos.puede('taxonomia.gestionar')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        eliminarCategoriaService.eliminar(id, actorId);
        return ResponseEntity.noContent().build();
    }
}
