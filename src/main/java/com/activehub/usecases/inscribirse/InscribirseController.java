package com.activehub.usecases.inscribirse;

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
@RequestMapping("/api/alumno/clases/{claseId}/inscripciones")
public class InscribirseController {

    private final InscribirseService inscribirseService;

    public InscribirseController(InscribirseService inscribirseService) {
        this.inscribirseService = inscribirseService;
    }

    @PostMapping
    @PreAuthorize("@permisos.puede('inscripciones.gestionar')")
    public ResponseEntity<InscribirseResponse> inscribirse(
            @PathVariable UUID claseId,
            @Valid @RequestBody InscribirseRequest request,
            Authentication authentication) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        InscribirseResponse response = inscribirseService.inscribirse(claseId, request, alumnoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
