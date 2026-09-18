package com.activehub.usecases.reenviarcodigoemail;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/verificar-email/reenviar")
public class ReenviarCodigoEmailController {

    private final ReenviarCodigoEmailService reenviarCodigoEmailService;

    public ReenviarCodigoEmailController(ReenviarCodigoEmailService reenviarCodigoEmailService) {
        this.reenviarCodigoEmailService = reenviarCodigoEmailService;
    }

    @PostMapping
    public ReenviarCodigoEmailResponse reenviar(Authentication authentication) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        return reenviarCodigoEmailService.reenviar(usuarioId);
    }
}
