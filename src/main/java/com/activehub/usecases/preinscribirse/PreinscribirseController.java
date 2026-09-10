package com.activehub.usecases.preinscribirse;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumno/clases/{claseId}/preinscripciones")
public class PreinscribirseController {

    private final PreinscribirseService preinscribirseService;

    public PreinscribirseController(PreinscribirseService preinscribirseService) {
        this.preinscribirseService = preinscribirseService;
    }

    @PostMapping
    @PreAuthorize("@permisos.puede('inscripciones.gestionar')")
    public ResponseEntity<PreinscribirseResponse> preinscribirse(
            @PathVariable UUID claseId, Authentication authentication) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        PreinscribirseResponse response = preinscribirseService.preinscribirse(claseId, alumnoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
