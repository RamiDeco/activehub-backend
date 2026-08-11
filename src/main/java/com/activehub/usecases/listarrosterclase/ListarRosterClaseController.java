package com.activehub.usecases.listarrosterclase;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/clases/{claseId}/roster")
public class ListarRosterClaseController {

    private final ListarRosterClaseService listarRosterClaseService;

    public ListarRosterClaseController(ListarRosterClaseService listarRosterClaseService) {
        this.listarRosterClaseService = listarRosterClaseService;
    }

    @GetMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ListarRosterClaseResponse listar(@PathVariable UUID claseId, Authentication authentication) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        return listarRosterClaseService.listar(claseId, instructorId);
    }
}
