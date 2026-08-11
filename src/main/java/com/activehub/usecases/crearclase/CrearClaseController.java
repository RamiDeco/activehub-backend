package com.activehub.usecases.crearclase;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/actividades/{actividadId}/clases")
public class CrearClaseController {

    private final CrearClaseService crearClaseService;

    public CrearClaseController(CrearClaseService crearClaseService) {
        this.crearClaseService = crearClaseService;
    }

    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<CrearClaseResponse> crear(
            @PathVariable UUID actividadId,
            @Valid @RequestBody CrearClaseRequest request,
            Authentication authentication) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        CrearClaseResponse response = crearClaseService.crear(actividadId, request, instructorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
