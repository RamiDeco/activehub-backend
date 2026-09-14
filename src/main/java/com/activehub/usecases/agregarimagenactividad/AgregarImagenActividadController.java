package com.activehub.usecases.agregarimagenactividad;

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
public class AgregarImagenActividadController {

    private final AgregarImagenActividadService agregarImagenActividadService;
    private final PermisosService permisosService;

    public AgregarImagenActividadController(
            AgregarImagenActividadService agregarImagenActividadService, PermisosService permisosService) {
        this.agregarImagenActividadService = agregarImagenActividadService;
        this.permisosService = permisosService;
    }

    @PostMapping("/{id}/imagenes")
    @PreAuthorize("@permisos.puede('actividades.publicar')")
    public ResponseEntity<AgregarImagenActividadResponse> agregar(
            @PathVariable UUID id, @RequestParam("archivo") MultipartFile archivo, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        boolean puedeModerar = permisosService.puede(actorId, "actividades.moderar");
        AgregarImagenActividadResponse response = agregarImagenActividadService.agregar(id, archivo, actorId, puedeModerar);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
