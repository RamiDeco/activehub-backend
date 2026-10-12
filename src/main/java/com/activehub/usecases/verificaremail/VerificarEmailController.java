package com.activehub.usecases.verificaremail;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/verificar-email")
public class VerificarEmailController {

    private final VerificarEmailService verificarEmailService;

    public VerificarEmailController(VerificarEmailService verificarEmailService) {
        this.verificarEmailService = verificarEmailService;
    }

    @PostMapping
    public VerificarEmailResponse verificar(
            @Valid @RequestBody VerificarEmailRequest request, Authentication authentication) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        return verificarEmailService.verificar(usuarioId, request);
    }
}
