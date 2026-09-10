package com.activehub.usecases.aprobarinstructor;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/instructores")
public class AprobarInstructorController {

    private final AprobarInstructorService aprobarInstructorService;

    public AprobarInstructorController(AprobarInstructorService aprobarInstructorService) {
        this.aprobarInstructorService = aprobarInstructorService;
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("@permisos.puede('instructores.validar')")
    public AprobarInstructorResponse aprobar(@PathVariable UUID id, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return aprobarInstructorService.aprobar(id, actorId);
    }
}
