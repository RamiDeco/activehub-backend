package com.activehub.usecases.confirmarcobroefectivo;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/inscripciones")
public class ConfirmarCobroEfectivoController {

    private final ConfirmarCobroEfectivoService confirmarCobroEfectivoService;

    public ConfirmarCobroEfectivoController(ConfirmarCobroEfectivoService confirmarCobroEfectivoService) {
        this.confirmarCobroEfectivoService = confirmarCobroEfectivoService;
    }

    @PostMapping("/{id}/confirmar-cobro")
    @PreAuthorize("@permisos.puede('cobros.confirmar')")
    public ConfirmarCobroEfectivoResponse confirmar(@PathVariable UUID id, Authentication authentication) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        return confirmarCobroEfectivoService.confirmar(id, instructorId);
    }
}
