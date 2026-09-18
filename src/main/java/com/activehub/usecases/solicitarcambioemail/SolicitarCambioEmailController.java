package com.activehub.usecases.solicitarcambioemail;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios/me/email")
public class SolicitarCambioEmailController {

    private final SolicitarCambioEmailService solicitarCambioEmailService;

    public SolicitarCambioEmailController(SolicitarCambioEmailService solicitarCambioEmailService) {
        this.solicitarCambioEmailService = solicitarCambioEmailService;
    }

    @PostMapping
    public SolicitarCambioEmailResponse solicitar(
            @Valid @RequestBody SolicitarCambioEmailRequest request, Authentication authentication) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        return solicitarCambioEmailService.solicitar(usuarioId, request);
    }
}
