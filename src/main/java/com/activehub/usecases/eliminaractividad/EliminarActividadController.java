package com.activehub.usecases.eliminaractividad;

import com.activehub.shared.security.PermisosService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/actividades")
public class EliminarActividadController {

    private final EliminarActividadService eliminarActividadService;
    private final PermisosService permisosService;

    public EliminarActividadController(
            EliminarActividadService eliminarActividadService, PermisosService permisosService) {
        this.eliminarActividadService = eliminarActividadService;
        this.permisosService = permisosService;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permisos.puede('actividades.publicar')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        boolean puedeModerar = permisosService.puede(actorId, "actividades.moderar");
        eliminarActividadService.eliminar(id, actorId, puedeModerar);
        return ResponseEntity.noContent().build();
    }
}
