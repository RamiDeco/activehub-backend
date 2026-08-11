package com.activehub.usecases.eliminartipoactividad;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tipos-actividad")
public class EliminarTipoActividadController {

    private final EliminarTipoActividadService eliminarTipoActividadService;

    public EliminarTipoActividadController(EliminarTipoActividadService eliminarTipoActividadService) {
        this.eliminarTipoActividadService = eliminarTipoActividadService;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        eliminarTipoActividadService.eliminar(id, actorId);
        return ResponseEntity.noContent().build();
    }
}
