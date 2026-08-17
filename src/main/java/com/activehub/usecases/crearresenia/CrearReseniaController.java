package com.activehub.usecases.crearresenia;

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
@RequestMapping("/api/alumno/clases")
public class CrearReseniaController {

    private final CrearReseniaService crearReseniaService;

    public CrearReseniaController(CrearReseniaService crearReseniaService) {
        this.crearReseniaService = crearReseniaService;
    }

    @PostMapping("/{claseId}/resenas")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<CrearReseniaResponse> crear(
            @PathVariable UUID claseId, @Valid @RequestBody CrearReseniaRequest request, Authentication authentication
    ) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        CrearReseniaResponse response = crearReseniaService.crear(claseId, request, alumnoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
