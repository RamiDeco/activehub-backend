package com.activehub.usecases.creartipoactividad;

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
@RequestMapping("/api/admin/tipos-actividad")
public class CrearTipoActividadController {

    private final CrearTipoActividadService crearTipoActividadService;

    public CrearTipoActividadController(CrearTipoActividadService crearTipoActividadService) {
        this.crearTipoActividadService = crearTipoActividadService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CrearTipoActividadResponse> crear(
            @Valid @RequestBody CrearTipoActividadRequest request, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        CrearTipoActividadResponse response = crearTipoActividadService.crear(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
