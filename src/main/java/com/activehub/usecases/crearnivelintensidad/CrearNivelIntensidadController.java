package com.activehub.usecases.crearnivelintensidad;

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
@RequestMapping("/api/admin/niveles-intensidad")
public class CrearNivelIntensidadController {

    private final CrearNivelIntensidadService crearNivelIntensidadService;

    public CrearNivelIntensidadController(CrearNivelIntensidadService crearNivelIntensidadService) {
        this.crearNivelIntensidadService = crearNivelIntensidadService;
    }

    @PostMapping
    @PreAuthorize("@permisos.puede('taxonomia.gestionar')")
    public ResponseEntity<CrearNivelIntensidadResponse> crear(
            @Valid @RequestBody CrearNivelIntensidadRequest request, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        CrearNivelIntensidadResponse response = crearNivelIntensidadService.crear(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
