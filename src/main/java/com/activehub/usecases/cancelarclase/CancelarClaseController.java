package com.activehub.usecases.cancelarclase;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/clases")
public class CancelarClaseController {

    private final CancelarClaseService cancelarClaseService;

    public CancelarClaseController(CancelarClaseService cancelarClaseService) {
        this.cancelarClaseService = cancelarClaseService;
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public CancelarClaseResponse cancelar(@PathVariable UUID id, Authentication authentication) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        return cancelarClaseService.cancelar(id, instructorId);
    }
}
