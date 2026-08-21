package com.activehub.usecases.notificarausenciaprofesor;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/clases")
public class NotificarAusenciaProfesorController {

    private final NotificarAusenciaProfesorService notificarAusenciaProfesorService;

    public NotificarAusenciaProfesorController(NotificarAusenciaProfesorService notificarAusenciaProfesorService) {
        this.notificarAusenciaProfesorService = notificarAusenciaProfesorService;
    }

    @PostMapping("/{id}/notificar-ausencia")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public NotificarAusenciaProfesorResponse notificar(
            @PathVariable UUID id, @Valid @RequestBody NotificarAusenciaProfesorRequest request,
            Authentication authentication) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        return notificarAusenciaProfesorService.notificar(id, instructorId, request);
    }
}
