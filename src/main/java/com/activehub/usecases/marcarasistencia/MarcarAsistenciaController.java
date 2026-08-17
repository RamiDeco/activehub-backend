package com.activehub.usecases.marcarasistencia;

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
@RequestMapping("/api/instructor/inscripciones")
public class MarcarAsistenciaController {

    private final MarcarAsistenciaService marcarAsistenciaService;

    public MarcarAsistenciaController(MarcarAsistenciaService marcarAsistenciaService) {
        this.marcarAsistenciaService = marcarAsistenciaService;
    }

    @PostMapping("/{id}/asistencia")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public MarcarAsistenciaResponse marcar(
            @PathVariable UUID id,
            @Valid @RequestBody MarcarAsistenciaRequest request,
            Authentication authentication
    ) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        return marcarAsistenciaService.marcar(id, request, instructorId);
    }
}
