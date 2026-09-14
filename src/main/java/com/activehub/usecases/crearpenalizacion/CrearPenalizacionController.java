package com.activehub.usecases.crearpenalizacion;

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
@RequestMapping("/api/admin/penalizaciones")
public class CrearPenalizacionController {

    private final CrearPenalizacionService crearPenalizacionService;

    public CrearPenalizacionController(CrearPenalizacionService crearPenalizacionService) {
        this.crearPenalizacionService = crearPenalizacionService;
    }

    @PostMapping
    @PreAuthorize("@permisos.puede('penalizaciones.gestionar')")
    public ResponseEntity<CrearPenalizacionResponse> crear(
            @Valid @RequestBody CrearPenalizacionRequest request, Authentication authentication
    ) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(crearPenalizacionService.crear(request, actorId));
    }
}
