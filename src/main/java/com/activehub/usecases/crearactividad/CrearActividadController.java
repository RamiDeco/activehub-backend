package com.activehub.usecases.crearactividad;

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
@RequestMapping("/api/instructor/actividades")
public class CrearActividadController {

    private final CrearActividadService crearActividadService;

    public CrearActividadController(CrearActividadService crearActividadService) {
        this.crearActividadService = crearActividadService;
    }

    @PostMapping
    @PreAuthorize("@permisos.puede('actividades.publicar')")
    public ResponseEntity<CrearActividadResponse> crear(
            @Valid @RequestBody CrearActividadRequest request, Authentication authentication) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        CrearActividadResponse response = crearActividadService.crear(request, instructorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
