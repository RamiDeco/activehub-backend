package com.activehub.usecases.rechazarinstructor;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/instructores")
public class RechazarInstructorController {

    private final RechazarInstructorService rechazarInstructorService;

    public RechazarInstructorController(RechazarInstructorService rechazarInstructorService) {
        this.rechazarInstructorService = rechazarInstructorService;
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("@permisos.puede('instructores.validar')")
    public RechazarInstructorResponse rechazar(
            @PathVariable UUID id,
            @RequestBody(required = false) RechazarInstructorRequest request,
            Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        RechazarInstructorRequest body = request != null ? request : new RechazarInstructorRequest(null);
        return rechazarInstructorService.rechazar(id, body, actorId);
    }
}
