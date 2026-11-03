package com.activehub.usecases.desbloquearinstructorpenalizacion;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/penalizaciones")
public class DesbloquearInstructorPenalizacionController {

    private final DesbloquearInstructorPenalizacionService desbloquearInstructorPenalizacionService;

    public DesbloquearInstructorPenalizacionController(
            DesbloquearInstructorPenalizacionService desbloquearInstructorPenalizacionService) {
        this.desbloquearInstructorPenalizacionService = desbloquearInstructorPenalizacionService;
    }

    @PostMapping("/{id}/desbloquear")
    @PreAuthorize("@permisos.puede('penalizaciones.gestionar')")
    public DesbloquearInstructorPenalizacionResponse desbloquear(@PathVariable UUID id, Authentication authentication) {
        return desbloquearInstructorPenalizacionService.desbloquear(id, (UUID) authentication.getPrincipal());
    }
}
