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
    // El actor entra si publica lo suyo O si modera lo ajeno; el Service decide despues el
    // alcance con `puedeModerar`. Pedir solo `actividades.publicar` dejaba a
    // `actividades.moderar` muerto: desde V22 el ADMIN modera pero no publica, asi que la
    // guarda lo frenaba antes de llegar a la linea que pregunta si puede moderar.
    @PreAuthorize("@permisos.puede('actividades.publicar') or @permisos.puede('actividades.moderar')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        boolean puedeModerar = permisosService.puede(actorId, "actividades.moderar");
        eliminarActividadService.eliminar(id, actorId, puedeModerar);
        return ResponseEntity.noContent().build();
    }
}
