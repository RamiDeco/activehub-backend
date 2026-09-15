package com.activehub.usecases.subirfotoactividad;

import com.activehub.shared.security.PermisosService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/instructor/actividades")
public class SubirFotoActividadController {

    private final SubirFotoActividadService subirFotoActividadService;
    private final PermisosService permisosService;

    public SubirFotoActividadController(
            SubirFotoActividadService subirFotoActividadService, PermisosService permisosService) {
        this.subirFotoActividadService = subirFotoActividadService;
        this.permisosService = permisosService;
    }

    @PostMapping("/{id}/foto")
    // El actor entra si publica lo suyo O si modera lo ajeno; el Service decide despues el
    // alcance con `puedeModerar`. Pedir solo `actividades.publicar` dejaba a
    // `actividades.moderar` muerto: desde V22 el ADMIN modera pero no publica, asi que la
    // guarda lo frenaba antes de llegar a la linea que pregunta si puede moderar.
    @PreAuthorize("@permisos.puede('actividades.publicar') or @permisos.puede('actividades.moderar')")
    public ResponseEntity<SubirFotoActividadResponse> subir(
            @PathVariable UUID id, @RequestParam("archivo") MultipartFile archivo, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        boolean puedeModerar = permisosService.puede(actorId, "actividades.moderar");
        SubirFotoActividadResponse response = subirFotoActividadService.subir(id, archivo, actorId, puedeModerar);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
