package com.activehub.usecases.crearrol;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/roles")
public class CrearRolController {

    private final CrearRolService crearRolService;

    public CrearRolController(CrearRolService crearRolService) {
        this.crearRolService = crearRolService;
    }

    @PostMapping
    @PreAuthorize("@permisos.puede('roles.configurar')")
    public ResponseEntity<CrearRolResponse> crear(
            @Valid @RequestBody CrearRolRequest request, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(crearRolService.crear(request, actorId));
    }
}
